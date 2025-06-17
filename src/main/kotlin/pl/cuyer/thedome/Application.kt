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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
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


fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
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
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017"
    val client = KMongo.createClient(mongoUri).coroutine
    val database = client.getDatabase("thedome")
    val serversCollection = database.getCollection<BattlemetricsServerContent>("servers")

    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    monitor.subscribe(ApplicationStarted) {
        launchFetchJob(httpClient, serversCollection)
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

private fun Application.launchFetchJob(
    httpClient: HttpClient,
    serversCollection: CoroutineCollection<BattlemetricsServerContent>
) {
    val delayMillis = (System.getenv("FETCH_DELAY_MS") ?: "3600000").toLong()
    val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    monitor.subscribe(ApplicationStopped) {
        fetchScope.cancel()
    }
    fetchScope.launch {
        while (true) {
            try {
                fetchServers(httpClient, serversCollection)
            } catch (e: Exception) {
                log.error("Failed to fetch servers", e)
            }
            delay(delayMillis)
        }
    }
}

private suspend fun fetchServers(
    client: HttpClient,
    collection: CoroutineCollection<BattlemetricsServerContent>
) {
    val servers = mutableListOf<BattlemetricsServerContent>()
    val apiKey = System.getenv("API_KEY") ?: ""
    var url: String? = "https://api.battlemetrics.com/servers?filter[game]=rust&sort=rank&page[size]=100"
    while (url != null) {
        val page: BattlemetricsPage = client.get(url).body()
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
    }
}

