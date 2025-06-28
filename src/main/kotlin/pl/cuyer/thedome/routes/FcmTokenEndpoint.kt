package pl.cuyer.thedome.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import pl.cuyer.thedome.domain.fcm.FcmTokenRequest
import pl.cuyer.thedome.services.FcmTokenService

class FcmTokenEndpoint(private val service: FcmTokenService) {
    fun register(route: Route) {
        with(route) {
            authenticate("auth-jwt") {
                post("/fcm/token") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val req = call.receive<FcmTokenRequest>()
                    service.registerToken(username, req.token)
                    call.respond(HttpStatusCode.Created)
                }
                delete("/fcm/token/{token}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val token = call.parameters["token"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    service.removeToken(username, token)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

