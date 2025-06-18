package pl.cuyer.thedome

import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoClient
import dev.inmo.krontab.builder.SchedulerBuilder
import dev.inmo.krontab.builder.TimeBuilder
import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskScheduling
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.mongoDb
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.battlemetrics.*
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServerFetchService
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.routes.ServersEndpoint
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private val logger = LoggerFactory.getLogger("pl.cuyer.thedome.Application")


fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting server on port $port")
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
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

    val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017"
    val mongoClient = KMongo.createClient(mongoUri).coroutine
    val database = mongoClient.getDatabase("thedome")
    val serversCollection = database.getCollection<BattlemetricsServerContent>("servers")
    runBlocking {
        serversCollection.createIndex("{ 'attributes.rank': 1 }")
        serversCollection.createIndex("{ 'attributes.country': 1 }")
        serversCollection.createIndex("{ 'attributes.details.rust_settings.timeZone': 1 }")
        serversCollection.createIndex("{ 'attributes.details.rust_gamemode': 1 }")
        serversCollection.createIndex("{ 'attributes.details.rust_type': 1 }")
        serversCollection.createIndex("{ 'attributes.details.official': 1 }")
        serversCollection.createIndex("{ 'attributes.details.rust_settings.groupLimit': 1 }")
        serversCollection.createIndex("{ 'attributes.details.rust_last_wipe': 1 }")
    }

    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val fetchService = ServerFetchService(httpClient, serversCollection)

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

    val serversService = ServersService(serversCollection)
    val serversEndpoint = ServersEndpoint(serversService)

    routing {
        serversEndpoint.register(this)
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

