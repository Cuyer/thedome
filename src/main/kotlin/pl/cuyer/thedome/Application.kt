package pl.cuyer.thedome

import com.mongodb.kotlin.client.coroutine.MongoClient
import dev.inmo.krontab.builder.SchedulerBuilder
import dev.inmo.krontab.builder.TimeBuilder
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskScheduling
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.mongoDb
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics

import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.battlemetrics.*
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServerFetchService
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.services.FiltersService
import pl.cuyer.thedome.services.AuthService
import pl.cuyer.thedome.routes.ServersEndpoint
import pl.cuyer.thedome.routes.FiltersEndpoint
import pl.cuyer.thedome.routes.AuthEndpoint
import pl.cuyer.thedome.plugins.AnonymousRateLimit
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.slf4jLogger
import pl.cuyer.thedome.di.appModule
import pl.cuyer.thedome.AppConfig
import pl.cuyer.thedome.domain.ErrorResponse
import pl.cuyer.thedome.exceptions.UserAlreadyExistsException
import pl.cuyer.thedome.exceptions.InvalidCredentialsException
import pl.cuyer.thedome.exceptions.InvalidRefreshTokenException
import pl.cuyer.thedome.exceptions.AnonymousUpgradeException
import pl.cuyer.thedome.exceptions.FiltersOptionsException
import pl.cuyer.thedome.exceptions.ServersQueryException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

private const val API_VERSION = "1.0.0"
private val logger = LoggerFactory.getLogger("pl.cuyer.thedome.Application")


fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting server on port $port")
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    val config = AppConfig.load(environment.config)
    install(Koin) {
        slf4jLogger()
        modules(appModule(config))
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Resources)
    install(StatusPages) {
        exception<UserAlreadyExistsException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Conflict"))
        }
        exception<InvalidCredentialsException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Unauthorized"))
        }
        exception<InvalidRefreshTokenException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message ?: "Unauthorized"))
        }
        exception<AnonymousUpgradeException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Conflict"))
        }
        exception<FiltersOptionsException> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal server error"))
        }
        exception<ServersQueryException> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal server error"))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal server error"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("Page Not Found"))
        }
    }
    install(CallLogging) {
        filter { call -> call.request.path().startsWith("/servers") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent"
        }
    }

    install(CORS) {
        val allowedOriginsEnv = config.allowedOrigins
        if (allowedOriginsEnv.isNullOrBlank()) {
            anyHost()
        } else {
            allowedOriginsEnv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { origin ->
                val uri = try { java.net.URI(origin) } catch (_: Exception) { null }
                if (uri != null && uri.host != null) {
                    allowHost(uri.host, schemes = listOfNotNull(uri.scheme))
                } else {
                    allowHost(origin)
                }
            }
        }
        allowHeader(HttpHeaders.ContentType)
    }

    val jwtAudience = config.jwtAudience
    val jwtIssuer = config.jwtIssuer
    val jwtRealm = config.jwtRealm
    val jwtSecret = config.jwtSecret

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }

    install(AnonymousRateLimit) {
        requestsPerMinute = 60
    }

    val fetchService by inject<ServerFetchService>()

    val schedulerClient = MongoClient.create(config.mongoUri)
    logger.info("Scheduling fetch task with cron expression '${config.fetchCron}'")

    install(TaskScheduling) {
        mongoDb {
            client = schedulerClient
            databaseName = "thedome"
        }
        task {
            name = "fetch-servers"
            kronSchedule = { applyCron(config.fetchCron) }
            task = { fetchService.fetchServers() }
        }
    }

    val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = metricsRegistry
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics()
        )
    }

    monitor.subscribe(ApplicationStopped) {
        schedulerClient.close()
    }

    val serversService by inject<ServersService>()
    val filtersService by inject<FiltersService>()
    val authService by inject<AuthService>()
    val serversEndpoint = ServersEndpoint(serversService)
    val filtersEndpoint = FiltersEndpoint(filtersService)
    val authEndpoint = AuthEndpoint(authService)

    routing {
        authEndpoint.register(this)

        authenticate("auth-jwt") {
            get("/") {
                call.respond(
                    mapOf(
                        "name" to "TheDome API",
                        "version" to API_VERSION,
                        "docs" to "/swagger"
                    )
                )
            }
            serversEndpoint.register(this)
            filtersEndpoint.register(this)
        }
        get("/metrics") { call.respondText(metricsRegistry.scrape()) }
        swaggerUI(path = "swagger")
    }
}


private fun SchedulerBuilder.applyCron(expr: String) {
    val parts = expr.trim().split(" ").filter { it.isNotEmpty() }
    require(parts.size >= 5) { "Cron expression must have at least 5 parts" }
    seconds { applyPart(parts[0]) }
    minutes { applyPart(parts[1]) }
    hours { applyPart(parts[2]) }
    dayOfMonth { applyPart(parts[3]) }
    months { applyPart(parts[4]) }
    if (parts.size > 5) {
        dayOfWeek { applyPart(parts[5]) }
    }
    if (parts.size > 6) {
        years { applyPart(parts[6]) }
    }
}

private fun <N : Number> TimeBuilder<N>.applyPart(token: String) {
    when {
        token == "*" || token == "?" -> allowAll()
        token.startsWith("*/") -> {
            val step = token.substring(2).toInt()
            every(step)
        }
        else -> at(token.toInt())
    }
}

