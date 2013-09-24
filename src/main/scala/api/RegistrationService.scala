package api

import spray.routing.Directives
import scala.concurrent.ExecutionContext
import akka.actor.ActorRef
import core.{User, RegistrationActor}
import akka.util.Timeout
import RegistrationActor._
import spray.http.{StatusCodes, StatusCode}
import api.scaffolding.DefaultJsonFormats

class RegistrationService(registration: ActorRef)(implicit executionContext: ExecutionContext)
  extends Directives with DefaultJsonFormats {

  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = Timeout(2.seconds)

  implicit val userFormat = jsonFormat4(User)
  implicit val registerFormat = jsonFormat1(Register)
  implicit val registeredFormat = jsonObjectFormat[Registered.type]
  implicit val notRegisteredFormat = jsonObjectFormat[NotRegistered.type]

  //We could leave this out and it'd default to the DefaultRequestRejectionErrorSelector
  implicit object EitherErrorSelector extends ErrorSelector[NotRegistered.type] {
    def apply(v: NotRegistered.type): StatusCode = StatusCodes.BadRequest
  }

  val route =
    path("register") {
      post {
        handleWith { ru: Register => (registration ? ru).mapTo[Either[NotRegistered.type, Registered.type]] }
      }
    }

}
