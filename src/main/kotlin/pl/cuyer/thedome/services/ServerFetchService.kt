package pl.cuyer.thedome.services

import com.mongodb.client.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.litote.kmongo.coroutine.CoroutineCollection
import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsPage
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.extractMapId
import pl.cuyer.thedome.domain.battlemetrics.fetchMapIcon

class ServerFetchService(
    private val client: HttpClient,
    private val collection: CoroutineCollection<BattlemetricsServerContent>,
    private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(ServerFetchService::class.java)

    suspend fun fetchServers() {
        val servers = mutableListOf<BattlemetricsServerContent>()
        val iconCache = mutableMapOf<String, String?>()
        var url: String? = "https://api.battlemetrics.com/servers?sort=rank&filter[game]=rust&page[size]=100&filter[status]=online,offline"
        logger.info("Fetching servers from Battlemetrics API")

        try {
            while (url != null) {
                logger.info("Requesting page: $url")
                val page: BattlemetricsPage = client.get(url).body()
                logger.info("Received ${page.data.size} servers")

                val pageServers = coroutineScope {
                    page.data.map { server ->
                        async {
                            val mapId = server.extractMapId()
                            val iconUrl = if (!mapId.isNullOrEmpty() && apiKey.isNotEmpty() && server.attributes.status != "offline") {
                                iconCache.getOrPut(mapId) { client.fetchMapIcon(mapId, apiKey) }
                            } else null

                            val details = server.attributes.details
                            val rustMaps = details?.rustMaps
                            val newRustMaps = if (iconUrl != null) rustMaps?.copy(imageIconUrl = iconUrl) else rustMaps
                            val newDetails = details?.copy(
                                rustMaps = newRustMaps
                            )
                            server.copy(attributes = server.attributes.copy(details = newDetails))
                        }
                    }.awaitAll()
                }
                val currentYear = Clock.System.now().toLocalDateTime(TimeZone.UTC).year
                val filteredServers = pageServers.filter { server ->
                    val year = server.attributes.updatedAt
                        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                        ?.toLocalDateTime(TimeZone.UTC)
                        ?.year
                    year == null || year >= currentYear
                }
                servers += filteredServers

                url = page.links?.next
            }

            if (servers.isEmpty()) {
                logger.warn("No servers fetched. Skipping database update.")
                return
            }

            val ids = servers.map { it.id }
            val existing = collection
                .find(Filters.`in`("id", ids))
                .toList()
                .associateBy { it.id }

            val replaceOperations = servers.mapNotNull { server ->
                val old = existing[server.id]
                val newTime = server.attributes.updatedAt?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                }
                val oldTime = old?.attributes?.updatedAt?.let {
                    runCatching { Instant.parse(it) }.getOrNull()
                }
                if (old == null || (newTime != null && oldTime != null && newTime > oldTime)) {
                    ReplaceOneModel(
                        Filters.eq("id", server.id),
                        server,
                        ReplaceOptions().upsert(true)
                    )
                } else {
                    null
                }
            }

            if (replaceOperations.isNotEmpty()) {
                collection.bulkWrite(replaceOperations, BulkWriteOptions().ordered(false))
            }

            val idsToKeep = servers.map { it.id }
            collection.deleteMany(Filters.nin("id", idsToKeep))

            logger.info("Upserted ${idsToKeep.size} servers and removed stale entries.")

        } catch (e: Exception) {
            logger.error("Failed to fetch or update servers: ${e.message}", e)
        }
    }
}
