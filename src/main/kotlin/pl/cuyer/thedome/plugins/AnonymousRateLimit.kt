package pl.cuyer.thedome.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.*
import pl.cuyer.thedome.domain.ErrorResponse
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

class AnonymousRateLimitConfig {
    var requestsPerMinute: Int = 60
    var cleanupIntervalMillis: Long = 60_000
    var requestTtlMillis: Long = 60_000
}

internal class RateInfo(var count: Int, var startMillis: Long)

internal fun launchCleanupJob(
    scope: CoroutineScope,
    requests: ConcurrentHashMap<String, RateInfo>,
    ttl: Long,
    interval: Long
): Job = scope.launch {
    while (isActive) {
        delay(interval)
        val now = System.currentTimeMillis()
        requests.entries.removeIf { now - it.value.startMillis >= ttl }
    }
}

val AnonymousRateLimit = createApplicationPlugin(
    name = "AnonymousRateLimit",
    createConfiguration = ::AnonymousRateLimitConfig
) {
    val requests = ConcurrentHashMap<String, RateInfo>()
    val cleanupJob = launchCleanupJob(
        application,
        requests,
        pluginConfig.requestTtlMillis,
        pluginConfig.cleanupIntervalMillis
    )
    environment.monitor.subscribe(ApplicationStopped) { cleanupJob.cancel() }

    onCall { call ->
        val principal = call.principal<JWTPrincipal>() ?: return@onCall
        val username = principal.getClaim("username", String::class) ?: return@onCall
        if (!username.startsWith("anon-")) return@onCall
        val now = System.currentTimeMillis()
        val info = requests.compute(username) { _, existing ->
            if (existing == null || now - existing.startMillis >= pluginConfig.requestTtlMillis) {
                RateInfo(1, now)
            } else {
                existing.count += 1
                existing
            }
        }!!
        if (info.count > pluginConfig.requestsPerMinute) {
            call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("Too many requests"))
            return@onCall
        }
    }
}
