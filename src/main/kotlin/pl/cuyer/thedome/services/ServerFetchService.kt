package pl.cuyer.thedome.services

import com.mongodb.client.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.litote.kmongo.coroutine.CoroutineCollection
import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsPage
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.extractMapId
import pl.cuyer.thedome.domain.battlemetrics.fetchMapIcon

class ServerFetchService(
    private val client: HttpClient,
    private val collection: CoroutineCollection<BattlemetricsServerContent>
) {
    private val logger = LoggerFactory.getLogger(ServerFetchService::class.java)

    suspend fun fetchServers() {
        val servers = mutableListOf<BattlemetricsServerContent>()
        val apiKey = System.getenv("API_KEY") ?: ""
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
                            val newGamemode = when (details?.rustGamemode?.lowercase()) {
                                "rust" -> "vanilla"
                                else -> details?.rustGamemode
                            }
                            val newDetails = details?.copy(
                                rustMaps = newRustMaps,
                                rustGamemode = newGamemode
                            )
                            server.copy(attributes = server.attributes.copy(details = newDetails))
                        }
                    }.awaitAll()
                }
                servers += pageServers

                url = page.links?.next
            }

            if (servers.isEmpty()) {
                logger.warn("No servers fetched. Skipping database update.")
                return
            }

            val replaceOperations = servers.map { server ->
                ReplaceOneModel(
                    Filters.eq("id", server.id),
                    server,
                    ReplaceOptions().upsert(true)
                )
            }
            collection.bulkWrite(replaceOperations, BulkWriteOptions().ordered(false))

            val idsToKeep = servers.map { it.id }
            collection.deleteMany(Filters.nin("id", idsToKeep))

            logger.info("Upserted ${servers.size} servers and removed stale entries.")

        } catch (e: Exception) {
            logger.error("Failed to fetch or update servers: ${e.message}", e)
        }
    }
}
