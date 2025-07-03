package pl.cuyer.thedome.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import pl.cuyer.thedome.domain.auth.LoginRequest
import pl.cuyer.thedome.domain.auth.RegisterRequest
import pl.cuyer.thedome.domain.auth.RefreshRequest
import pl.cuyer.thedome.domain.auth.UpgradeRequest
import pl.cuyer.thedome.domain.auth.GoogleAuthRequest
import pl.cuyer.thedome.domain.auth.DeleteAccountRequest
import pl.cuyer.thedome.domain.auth.ChangePasswordRequest
import pl.cuyer.thedome.domain.auth.EmailExistsResponse
import pl.cuyer.thedome.exceptions.AnonymousUpgradeException
import pl.cuyer.thedome.exceptions.InvalidCredentialsException
import pl.cuyer.thedome.exceptions.InvalidRefreshTokenException
import pl.cuyer.thedome.exceptions.UserAlreadyExistsException
import pl.cuyer.thedome.services.AuthService

class AuthEndpoint(private val service: AuthService) {
    fun register(root: Route) {
        with(root) {
            route("/auth") {
                post("/register") {
                    val req = call.receive<RegisterRequest>()
                    val tokens = service.register(req.username, req.email, req.password)
                        ?: throw UserAlreadyExistsException()
                    call.respond(tokens)
                }
                post("/anonymous") {
                    call.respond(service.registerAnonymous())
                }
                authenticate("auth-jwt") {
                    post("/upgrade") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val currentUsername = principal.getClaim("username", String::class)!!
                        val req = call.receive<UpgradeRequest>()
                        val tokens = if (req.token != null) {
                            service.upgradeAnonymousWithGoogle(currentUsername, req.token)
                        } else {
                            if (req.username == null || req.password == null || req.email == null) {
                                return@post call.respond(HttpStatusCode.BadRequest)
                            }
                            service.upgradeAnonymous(currentUsername, req.username, req.password, req.email)
                        } ?: throw AnonymousUpgradeException()
                        call.respond(tokens)
                    }
                }
                post("/login") {
                    val req = call.receive<LoginRequest>()
                    val tokens = service.login(req.username, req.password)
                        ?: throw InvalidCredentialsException()
                    call.respond(tokens)
                }
                post("/google") {
                    val req = call.receive<GoogleAuthRequest>()
                    val tokens = service.loginWithGoogle(req.token)
                        ?: throw InvalidCredentialsException()
                    call.respond(tokens)
                }
                get("/email-exists") {
                    val email = call.request.queryParameters["email"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val provider = service.emailExists(email)
                    call.respond(EmailExistsResponse(provider != null, provider))
                }
                post("/refresh") {
                    val req = call.receive<RefreshRequest>()
                    val tokens = service.refresh(req.refreshToken)
                        ?: throw InvalidRefreshTokenException()
                    call.respond(tokens)
                }
                authenticate("auth-jwt") {
                    post("/logout") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val username = principal.getClaim("username", String::class)!!
                        val result = service.logout(username)
                        if (!result) throw InvalidCredentialsException()
                        call.respond(HttpStatusCode.NoContent)
                    }
                    post("/delete") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val username = principal.getClaim("username", String::class)!!
                        val req = try { call.receive<DeleteAccountRequest>() } catch (_: Exception) { null }
                        val password = req?.password
                        val deleted = service.deleteAccount(username, password)
                        if (!deleted) throw InvalidCredentialsException()
                        call.respond(HttpStatusCode.NoContent)
                    }
                    post("/password") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val username = principal.getClaim("username", String::class)!!
                        val req = call.receive<ChangePasswordRequest>()
                        val changed = service.changePassword(username, req.oldPassword, req.newPassword)
                        if (!changed) throw InvalidCredentialsException()
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
