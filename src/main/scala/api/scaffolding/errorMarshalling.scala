package api.scaffolding

import spray.http._
import spray.routing._
import spray.util.LoggingContext
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import scala.util.control.NonFatal
import spray.json.DefaultJsonProtocol
import spray.httpx.marshalling.{CollectingMarshallingContext, Marshaller, MetaMarshallers}
import spray.http.HttpResponse


/**
 * Holds potential error response with the HTTP status and optional body
 *
 * @param responseStatus the status code
 * @param response the optional body
 */
case class ErrorResponseException(responseStatus: StatusCode, response: Option[HttpEntity]) extends Exception


/**
 * Top Level Rejection Trait, extend to define a rejection class for returning to the client.
 */
trait RequestRejection extends RuntimeException {
  def reason: String
}

/**
 * Generic response to Marshal for sending rejection information back to the client.
 * @param reason details of the rejection.
 */
case class GenericRejection(reason: String) extends RequestRejection

/**
 * Rejection used when an internal error occurs to provide further details to the client..
 * @param reason details of the rejection.
 */
case class InternalServerProblem(reason: String, detail: String) extends RequestRejection

/**
 * Provides a hook to catch exceptions and rejections from routes, allowing custom
 * responses to be provided, logs to be captured, and potentially remedial actions.
 *
 * Catches both [[scala.util.Left]] and Timeout errors, marshalling them as JSON.
 *
 * Note that this is not marshalled, but it is possible to do so allowing for a fully
 * JSON API (e.g. see how Foursquare do it).
 */
trait FailureHandling {
  this: HttpService =>

  /**
   * Wrap all default Rejections in the [[api.scaffolding.GenericRejection]] class then marshal as json.
   */
  val rejectionHandler = RejectionHandler {
    case rejections => jsonify(RejectionHandler.Default(rejections))
  }

  //Sends a GenericRejection to be marshalled and returned.
  def jsonify: Directive0 = routeRouteResponse {
    case HttpResponse(status, entity, _, _) => ctx =>
      ctx.complete(status, GenericRejection(if (entity != null) entity.asString else "An internal server error occurred, please contact the system administrator"))
  }

  /**
   * Defines exception handlers to marshal Errors into Json.
   * Additionally provides an ExceptionHandler case to handle the [[api.scaffolding.ErrorResponseException]] object.
   * This partial function would be automatically picked up by sprays exception handling mechanism if defined as implicit.
   */
  def exceptionHandler(implicit settings: RoutingSettings, log: LoggingContext) = ExceptionHandler {
    case NonFatal(ErrorResponseException(statusCode, entity)) => ctx =>
      ctx.complete(statusCode, entity)

    case NonFatal(e) ⇒ ctx ⇒
      log.error(e, "Error during processing of request {}", ctx.request)
      //TODO you may or may not want to hide the exception message so as not to give out internal details.
      ctx.complete(InternalServerError, InternalServerProblem(InternalServerError.defaultMessage, e.getMessage))
  }
}

trait EitherErrorMarshalling extends DefaultJsonProtocol with SprayJsonSupport with MetaMarshallers {

  /**
   * Type alias for function that converts ``A`` to some ``StatusCode``
   * @tparam A the type of the input values
   */
  type ErrorSelector[A] = A => StatusCode

  /**
   * Marshals instances of ``Either[A, B]`` into appropriate HTTP responses by marshalling the values
   * in the left or right projections; and by selecting the appropriate HTTP status code for the
   * values in the left projection.
   *
   * This is useful when you need to return validation or other processing errors, but need a bit more information than
   * just ``HTTP status 422`` (or, even worse simply ``400``).
   *
   * Bring an implicit instance of this method and an `ErrorSelector` into scope of your HttpServices to customise the status code.
   *
   * @param ma marshaller for the left projection
   * @param mb marshaller for the right projection
   * @param esa the selector converting the left projection to HTTP status code.
   *            The default will apply to any [[scala.util.Left]] if no overriding selector is defined - preventing unwanted status 200's.
   * @tparam A the left projection
   * @tparam B the right projection
   * @return marshaller
   */
  implicit def errorSelectingEitherMarshaller[A, B](implicit ma: Marshaller[A],
                                                    mb: Marshaller[B],
                                                    esa: ErrorSelector[A] = ((a: A) => StatusCodes.UnprocessableEntity)): Marshaller[Either[A, B]] =
    Marshaller[Either[A, B]] {
      (value, ctx) =>
        value match {
          case Left(a) =>
            val mc = new CollectingMarshallingContext()
            ma(a, mc)
            ctx.handleError(ErrorResponseException(esa(a), mc.entity))
          case Right(b) =>
            mb(b, ctx)
        }
    }
}

