package pl.cuyer.thedome.routes

import io.ktor.server.application.*
import io.ktor.server.resources.get
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.exceptions.ServersQueryException

class ServersEndpoint(private val service: ServersService) {
    fun register(route: Route) {
        with(route) {
            get<Servers> { params ->
                val username = call.principal<JWTPrincipal>()?.getClaim("username", String::class)
                val response = try {
                    service.getServers(params, username)
                } catch (e: Exception) {
                    throw ServersQueryException()
                }
                call.respond(response)
            }
        }
    }
}
