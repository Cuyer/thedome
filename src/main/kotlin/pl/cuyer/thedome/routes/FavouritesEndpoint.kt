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
import pl.cuyer.thedome.resources.Favourites
import pl.cuyer.thedome.services.FavouritesService
import pl.cuyer.thedome.exceptions.FavouriteLimitException

class FavouritesEndpoint(private val service: FavouritesService) {
    fun register(route: Route) {
        with(route) {
            authenticate("auth-jwt") {
                get<Favourites> { params ->
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val page = params.page ?: 1
                    val size = params.size ?: 20
                    val result = service.getFavourites(username, page, size)
                    call.respond(result)
                }
                post("/favourites/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val serverId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val added = service.addFavourite(username, serverId)
                    if (!added) throw FavouriteLimitException()
                    call.respond(HttpStatusCode.Created)
                }
                delete("/favourites/{id}") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val username = principal.getClaim("username", String::class)!!
                    val serverId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val removed = service.removeFavourite(username, serverId)
                    if (!removed) return@delete call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
