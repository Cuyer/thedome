package pl.cuyer.thedome.routes

import io.ktor.server.application.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServersService

class ServersEndpoint(private val service: ServersService) {
    fun register(route: Route) {
        with(route) {
            get<Servers> { params ->
                call.respond(service.getServers(params))
            }
        }
    }
}
