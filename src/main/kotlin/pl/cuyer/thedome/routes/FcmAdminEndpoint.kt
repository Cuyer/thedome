package pl.cuyer.thedome.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.cuyer.thedome.domain.fcm.FcmTopicRequest
import pl.cuyer.thedome.services.FcmService

class FcmAdminEndpoint(
    private val service: FcmService,
    private val apiKey: String
) {
    fun register(route: Route) {
        with(route) {
            post("/internal/notify") {
                val key = call.request.header("X-Api-Key")
                if (apiKey.isEmpty() || key != apiKey) {
                    return@post call.respond(HttpStatusCode.Unauthorized)
                }
                val req = call.receive<FcmTopicRequest>()
                service.sendToTopic(req.topic, req.name, req.type, req.timestamp)
                call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}
