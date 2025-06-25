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
import pl.cuyer.thedome.services.FavoritesService
import pl.cuyer.thedome.exceptions.ServersQueryException
import pl.cuyer.thedome.services.SubscriptionsService

class ServersEndpoint(
    private val service: ServersService,
    private val favoritesService: FavoritesService,
    private val subscriptionsService: SubscriptionsService
) {
    fun register(route: Route) {
        with(route) {
            authenticate("auth-jwt") {
                get<Servers> { params ->
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.getClaim("username", String::class)
                    val favorites = username?.let { favoritesService.getFavoriteIds(it) }
                    val subs = username?.let { subscriptionsService.getSubscriptions(it) }
                    val response = try {
                        service.getServers(params, favorites, subs)
                    } catch (e: Exception) {
                        throw ServersQueryException()
                    }
                    call.respond(response)
                }
            }
        }
    }
}
