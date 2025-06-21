package pl.cuyer.thedome.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import pl.cuyer.thedome.domain.auth.LoginRequest
import pl.cuyer.thedome.domain.auth.RegisterRequest
import pl.cuyer.thedome.domain.auth.RefreshRequest
import pl.cuyer.thedome.domain.auth.UpgradeRequest
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
                authenticate("auth-jwt") {
                    post("/upgrade") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val currentUsername = principal.getClaim("username", String::class)!!
                        val req = call.receive<UpgradeRequest>()
                        val tokens = service.upgradeAnonymous(currentUsername, req.username, req.password)
                        if (tokens != null) {
                            call.respond(tokens)
                        } else {
                            call.respond(HttpStatusCode.Conflict)
                        }
                    }
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
                post("/logout") {
                    val req = call.receive<RefreshRequest>()
                    service.logout(req.refreshToken)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
