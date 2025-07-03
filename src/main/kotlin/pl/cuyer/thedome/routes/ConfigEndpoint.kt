package pl.cuyer.thedome.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.cuyer.thedome.AppConfig

class ConfigEndpoint(private val config: AppConfig) {
    fun register(route: Route) {
        with(route) {
            get("/google-client-id") {
                call.respond(mapOf("googleClientId" to config.googleClientId))
            }
        }
    }
}
