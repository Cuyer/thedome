package pl.cuyer.thedome.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import pl.cuyer.thedome.resources.Favorites
import pl.cuyer.thedome.services.FavoritesService
import pl.cuyer.thedome.exceptions.FavoriteLimitException

class FavoritesEndpoint(private val service: FavoritesService) {
    fun register(route: Route) {
        with(route) {
            authenticate("auth-jwt") {
                get<Favorites> { params ->
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val page = params.page ?: 1
                    val size = params.size ?: 20
                    val result = service.getFavorites(username, page, size)
                    call.respond(result)
                }
                post("/favorites/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val serverId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val added = service.addFavorite(username, serverId)
                    if (!added) throw FavoriteLimitException()
                    call.respond(HttpStatusCode.Created)
                }
                delete("/favorites/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val serverId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val removed = service.removeFavorite(username, serverId)
                    if (!removed) return@delete call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
