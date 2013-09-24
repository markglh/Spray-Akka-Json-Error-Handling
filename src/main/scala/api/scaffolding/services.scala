package api.scaffolding

import spray.http.StatusCodes._
import spray.http._
import spray.routing._
import directives.{CompletionMagnet, RouteDirectives}
import spray.util.LoggingContext
import util.control.NonFatal
import spray.httpx.marshalling.Marshaller
import spray.http.HttpHeaders.RawHeader
import akka.actor.Actor

/**
 * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
 * @param routes the (concatenated) route
 */
class RoutedHttpService(routes: Route) extends HttpServiceActor with FailureHandling {

    //The failure and exception handling could be automatically picked up by spray by declaring the handlers as implicit.
  //Sometimes it's nicer to be explicit though imho!
  def receive = {
    runRoute(
      handleRejections(rejectionHandler)(
        handleExceptions(exceptionHandler)(
          routes
        )
      )
    )
  }
}

/**
 * Constructs ``CompletionMagnet``s that set the ``Access-Control-Allow-Origin`` header for modern browsers' AJAX
 * requests on different domains / ports.
 */
trait CrossLocationRouteDirectives extends RouteDirectives {

  implicit def fromObjectCross[T : Marshaller](origin: String)(obj: T) =
    new CompletionMagnet {
      def route: StandardRoute = new CompletionRoute(OK,
        RawHeader("Access-Control-Allow-Origin", origin) :: Nil, obj)
    }

  private class CompletionRoute[T : Marshaller](status: StatusCode, headers: List[HttpHeader], obj: T)
    extends StandardRoute {
    def apply(ctx: RequestContext): Unit = {
      ctx.complete(status, headers, obj)
    }
  }
}