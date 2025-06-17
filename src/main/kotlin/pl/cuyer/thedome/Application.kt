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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.litote.kmongo.coroutine.*
import org.litote.kmongo.reactivestreams.KMongo
import com.mongodb.client.model.Sorts
import kotlinx.serialization.json.JsonObject

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
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

    environment.monitor.subscribe(ApplicationStarted) {
        launchFetchJob(httpClient, serversCollection)
    }

    routing {
        get("/servers") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
            val skip = (page - 1) * size
            val servers = serversCollection
                .find()
                .sort(Sorts.ascending("attributes.rank"))
                .skip(skip)
                .limit(size)
                .toList()
            call.respond(servers)
        }
    }
}

private fun Application.launchFetchJob(
    httpClient: HttpClient,
    serversCollection: CoroutineCollection<BattlemetricsServerContent>
) {
    val delayMillis = (System.getenv("FETCH_DELAY_MS") ?: "3600000").toLong()
    val fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    environment.monitor.subscribe(ApplicationStopped) {
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
    var url: String? = "https://api.battlemetrics.com/servers?filter[game]=rust&sort=rank&page[size]=100"
    while (url != null) {
        val page: BattlemetricsPage = client.get(url).body()
        servers += page.data
        url = page.links?.next
    }

    collection.deleteMany()
    if (servers.isNotEmpty()) {
        collection.insertMany(servers)
    }
}

@Serializable
data class BattlemetricsPage(
    @SerialName("data")
    val data: List<BattlemetricsServerContent> = emptyList(),
    @SerialName("links")
    val links: Links? = null
)

@Serializable
data class Links(
    @SerialName("next")
    val next: String? = null
)

@Serializable
data class BattlemetricsServerContent(
    @SerialName("attributes")
    val attributes: Attributes,
    @SerialName("id")
    val id: String
)

@Serializable
data class Attributes(
    @SerialName("country")
    val country: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("details")
    val details: Details? = null,
    @SerialName("id")
    val id: String,
    @SerialName("ip")
    val ip: String? = null,
    @SerialName("maxPlayers")
    val maxPlayers: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("players")
    val players: Int? = null,
    @SerialName("port")
    val port: Int? = null,
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)

@Serializable
data class Details(
    @SerialName("map")
    val map: String? = null,
    @SerialName("official")
    val official: Boolean? = null,
    @SerialName("pve")
    val pve: Boolean? = null,
    @SerialName("rust_description")
    val rustDescription: String? = null,
    @SerialName("rust_fps_avg")
    val rustFpsAvg: Double? = null,
    @SerialName("rust_gamemode")
    val rustGamemode: String? = null,
    @SerialName("rust_headerimage")
    val rustHeaderimage: String? = null,
    @SerialName("rust_last_wipe")
    val rustLastWipe: String? = null,
    @SerialName("rust_maps")
    val rustMaps: JsonObject? = null,
    @SerialName("rust_type")
    val rustType: String? = null,
    @SerialName("rust_next_wipe")
    val rustNextWipe: String? = null,
    @SerialName("rust_next_wipe_map")
    val rustNextWipeMap: String? = null,
    @SerialName("rust_premium")
    val rustPremium: Boolean? = null,
    @SerialName("rust_queued_players")
    val rustQueuedPlayers: Int? = null,
    @SerialName("rust_settings")
    val rustSettings: JsonObject? = null,
    @SerialName("rust_url")
    val rustUrl: String? = null,
    @SerialName("rust_wipes")
    val rustWipes: List<JsonObject> = emptyList(),
    @SerialName("rust_world_seed")
    val rustWorldSeed: Long? = null,
    @SerialName("rust_world_size")
    val rustWorldSize: Int? = null
)
