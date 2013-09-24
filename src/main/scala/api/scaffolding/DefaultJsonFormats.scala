package api.scaffolding

import spray.json._
import java.util.UUID
import scala.reflect.ClassTag
import spray.httpx.marshalling.{MetaMarshallers, Marshaller, CollectingMarshallingContext}
import spray.http.{StatusCodes, StatusCode}
import spray.httpx.SprayJsonSupport
import core.RegistrationActor.NotRegistered
import spray.routing.ValidationRejection

/**
 * Contains useful JSON formats: ``j.u.Date``, ``j.u.UUID`` and others; it is useful
 * when creating traits that contain the ``JsonReader`` and ``JsonWriter`` instances
 * for types that contain ``Date``s, ``UUID``s and such like.
 */
trait DefaultJsonFormats extends DefaultJsonProtocol with SprayJsonSupport with MetaMarshallers with EitherErrorMarshalling {

  /**
   * Computes ``RootJsonFormat`` for type ``A`` if ``A`` is object,
   * we're using objects as return types rather than case classes so we need this to "bling" up the response.
   */
  def jsonObjectFormat[A: ClassTag]: RootJsonFormat[A] = new RootJsonFormat[A] {
    val ct = implicitly[ClassTag[A]]

    def write(obj: A): JsValue = JsObject("value" -> JsString(ct.runtimeClass.getSimpleName))

    def read(json: JsValue): A = ct.runtimeClass.newInstance().asInstanceOf[A]
  }

  /**
   * Instance of the ``RootJsonFormat`` for the ``j.u.UUID``
   */
  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID) = JsString(x.toString)

    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }

  trait RejectionFormats extends DefaultJsonProtocol {
    implicit val genericRejectionFormat = jsonFormat1(GenericRejection)
    implicit val internalProblemFormat = jsonFormat2(InternalServerProblem)
  }

}
