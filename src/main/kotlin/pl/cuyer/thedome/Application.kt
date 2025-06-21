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
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

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
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.inject
import org.koin.logger.slf4jLogger
import pl.cuyer.thedome.di.appModule
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
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "404: Page Not Found", status = status)
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
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "thedomeAudience"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "thedomeIssuer"
    val jwtRealm = System.getenv("JWT_REALM") ?: "thedomeRealm"
    val jwtSecret = System.getenv("JWT_SECRET") ?: "secret"

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

    val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017"

    val fetchService by inject<ServerFetchService>()

    val fetchCron = System.getenv("FETCH_CRON") ?: "0 */10 * * *"
    val schedulerClient = MongoClient.create(mongoUri)
    logger.info("Scheduling fetch task with cron expression '$fetchCron'")

    install(TaskScheduling) {
        mongoDb {
            client = schedulerClient
            databaseName = "thedome"
        }
        task {
            name = "fetch-servers"
            kronSchedule = { applyCron(fetchCron) }
            task = { fetchService.fetchServers() }
        }
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

