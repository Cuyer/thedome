package pl.cuyer.thedome.routes

import io.ktor.server.application.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.exceptions.ServersQueryException

class ServersEndpoint(private val service: ServersService) {
    fun register(route: Route) {
        with(route) {
            get<Servers> { params ->
                val response = try {
                    service.getServers(params)
                } catch (e: Exception) {
                    throw ServersQueryException()
                }
                call.respond(response)
            }
        }
    }
}
