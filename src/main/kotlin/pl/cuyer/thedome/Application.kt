package pl.cuyer.thedome

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*

import io.github.flaxoos.ktor.server.plugins.taskscheduling.TaskScheduling
import io.github.flaxoos.ktor.server.plugins.taskscheduling.managers.lock.database.mongoDb
import dev.inmo.krontab.builder.SchedulerBuilder
import dev.inmo.krontab.builder.buildSchedule
import dev.inmo.krontab.builder.TimeBuilder
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.litote.kmongo.coroutine.*
import org.litote.kmongo.reactivestreams.KMongo
import com.mongodb.client.model.Sorts
import pl.cuyer.thedome.domain.rust.RustMaps
import pl.cuyer.thedome.domain.rust.RustSettings
import pl.cuyer.thedome.domain.rust.RustWipe
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsPage
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.extractMapId
import pl.cuyer.thedome.domain.battlemetrics.fetchMapIcon
import pl.cuyer.thedome.domain.battlemetrics.toServerInfo
import io.ktor.server.resources.*
import pl.cuyer.thedome.resources.Servers
import org.slf4j.LoggerFactory

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
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
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

    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    val fetchCron = System.getenv("FETCH_CRON") ?: "0 0 * * * ? 0"
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
            task = { fetchServers(httpClient, serversCollection) }
        }
    }

    monitor.subscribe(ApplicationStopped) {
        schedulerClient.close()
    }

    routing {
        get<Servers> { servers: Servers ->
            val page = servers.page ?: 1
            val size = servers.size ?: 20
            val skip = (page - 1) * size
            val servers = serversCollection
                .find()
                .sort(Sorts.ascending("attributes.rank"))
                .skip(skip)
                .limit(size)
                .toList()
                .map { it.toServerInfo() }
            call.respond(servers)
        }
        swaggerUI(path = "swagger")
    }
}

private suspend fun fetchServers(
    client: HttpClient,
    collection: CoroutineCollection<BattlemetricsServerContent>
) {
    val servers = mutableListOf<BattlemetricsServerContent>()
    val apiKey = System.getenv("API_KEY") ?: ""
    var url: String? = "https://api.battlemetrics.com/servers?filter[game]=rust&sort=rank&page[size]=100"
    logger.info("Fetching servers from Battlemetrics API")
    while (url != null) {
        logger.info("Requesting page: $url")
        val page: BattlemetricsPage = client.get(url).body()
        logger.info("Received ${'$'}{page.data.size} servers")
        servers += page.data.map { server ->
            val mapId = server.extractMapId()
            if (!mapId.isNullOrEmpty() && apiKey.isNotEmpty()) {
                val iconUrl = client.fetchMapIcon(mapId, apiKey)
                val rustMaps = server.attributes.details?.rustMaps
                val details = server.attributes.details
                val newRustMaps = rustMaps?.copy(imageIconUrl = iconUrl)
                val newDetails = details?.copy(rustMaps = newRustMaps)
                server.copy(attributes = server.attributes.copy(details = newDetails))
            } else {
                server
            }
        }
        url = page.links?.next
    }

    collection.deleteMany()
    if (servers.isNotEmpty()) {
        collection.insertMany(servers)
        logger.info("Inserted ${'$'}{servers.size} servers into MongoDB")
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

