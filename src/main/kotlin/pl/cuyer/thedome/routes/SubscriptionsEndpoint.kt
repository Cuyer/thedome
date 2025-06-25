package pl.cuyer.thedome.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.cuyer.thedome.services.SubscriptionsService

class SubscriptionsEndpoint(private val service: SubscriptionsService) {
    fun register(route: Route) {
        with(route) {
            authenticate("auth-jwt") {
                get("/subscriptions") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val subs = service.getSubscriptions(username)
                    call.respond(subs)
                }
                post("/subscriptions/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val serverId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val added = service.subscribe(username, serverId)
                    if (!added) return@post call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.Created)
                }
                delete("/subscriptions/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val serverId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val removed = service.unsubscribe(username, serverId)
                    if (!removed) return@delete call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

