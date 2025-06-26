package pl.cuyer.thedome.routes

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.resources.get
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.services.FavouritesService
import pl.cuyer.thedome.exceptions.ServersQueryException
import pl.cuyer.thedome.services.SubscriptionsService

class ServersEndpoint(
    private val service: ServersService,
    private val favouritesService: FavouritesService,
    private val subscriptionsService: SubscriptionsService
) {
    fun register(route: Route) {
        with(route) {
            authenticate("auth-jwt") {
                get<Servers> { params ->
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.getClaim("username", String::class)
                    val favourites = username?.let { favouritesService.getFavouriteIds(it) }
                    val subs = username?.let { subscriptionsService.getSubscriptions(it) }
                    val response = try {
                        service.getServers(params, favourites, subs)
                    } catch (e: Exception) {
                        throw ServersQueryException()
                    }
                    call.respond(response)
                }
            }
        }
    }
}
