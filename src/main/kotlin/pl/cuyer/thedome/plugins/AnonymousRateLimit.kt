package pl.cuyer.thedome.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.*
import java.util.concurrent.ConcurrentHashMap

class AnonymousRateLimitConfig {
    var requestsPerMinute: Int = 60
}

private class RateInfo(var count: Int, var startMillis: Long)

val AnonymousRateLimit = createApplicationPlugin(
    name = "AnonymousRateLimit",
    createConfiguration = ::AnonymousRateLimitConfig
) {
    val requests = ConcurrentHashMap<String, RateInfo>()

    onCall { call ->
        val principal = call.principal<JWTPrincipal>() ?: return@onCall
        val username = principal.getClaim("username", String::class) ?: return@onCall
        if (!username.startsWith("anon-")) return@onCall
        val now = System.currentTimeMillis()
        val info = requests.compute(username) { _, existing ->
            if (existing == null || now - existing.startMillis >= 60_000) {
                RateInfo(1, now)
            } else {
                existing.count += 1
                existing
            }
        }!!
        if (info.count > pluginConfig.requestsPerMinute) {
            call.respond(HttpStatusCode.TooManyRequests)
            return@onCall
        }
    }
}
