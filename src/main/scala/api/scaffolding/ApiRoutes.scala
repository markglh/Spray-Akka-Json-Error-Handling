package api.scaffolding

import spray.routing.RouteConcatenation
import api.{MessengerService, RegistrationService}
import core.scaffolding.{Core, CoreActors}
import scala.concurrent.ExecutionContext

/**
 * @author markharrison
 */
trait ApiRoutes extends RouteConcatenation {
  this: CoreActors with Core =>

  protected implicit val _ = system.dispatcher

  val routes =
    new RegistrationService(registration).route ~
      new MessengerService(messenger).route

}
