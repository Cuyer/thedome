package pl.cuyer.thedome.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import pl.cuyer.thedome.domain.auth.LoginRequest
import pl.cuyer.thedome.domain.auth.RegisterRequest
import pl.cuyer.thedome.domain.auth.RefreshRequest
import pl.cuyer.thedome.services.AuthService

class AuthEndpoint(private val service: AuthService) {
    fun register(root: Route) {
        with(root) {
            route("/auth") {
                post("/register") {
                    val req = call.receive<RegisterRequest>()
                    val tokens = service.register(req.username, req.password)
                    if (tokens != null) {
                        call.respond(tokens)
                    } else {
                        call.respond(HttpStatusCode.Conflict)
                    }
                }
                post("/anonymous") {
                    call.respond(service.registerAnonymous())
                }
                post("/login") {
                    val req = call.receive<LoginRequest>()
                    val tokens = service.login(req.username, req.password)
                    if (tokens != null) {
                        call.respond(tokens)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
                post("/refresh") {
                    val req = call.receive<RefreshRequest>()
                    val tokens = service.refresh(req.refreshToken)
                    if (tokens != null) {
                        call.respond(tokens)
                    } else {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }
        }
    }
}
