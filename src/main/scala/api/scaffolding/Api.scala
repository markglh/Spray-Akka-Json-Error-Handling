package api.scaffolding

import akka.actor.Props
import core.scaffolding.{Core, CoreActors}

/**
 * The REST API layer. It exposes the REST services, but does not provide any
 * web server interface.<br/>
 * Notice that it requires to be mixed in with ``core.CoreActors``, which provides access
 * to the top-level actors that make up the system.
 */
trait Api extends ApiRoutes {
  this: CoreActors with Core =>

  val rootService = system.actorOf(Props(new RoutedHttpService(routes)))
}
