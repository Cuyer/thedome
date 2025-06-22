package pl.cuyer.thedome.routes

import io.ktor.server.application.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import pl.cuyer.thedome.resources.FiltersOptions
import pl.cuyer.thedome.services.FiltersService
import pl.cuyer.thedome.exceptions.FiltersOptionsException

class FiltersEndpoint(private val service: FiltersService) {
    fun register(route: Route) {
        with(route) {
            get<FiltersOptions> {
                val options = try {
                    service.getOptions()
                } catch (e: Exception) {
                    throw FiltersOptionsException()
                }
                call.respond(options)
            }
        }
    }
}
