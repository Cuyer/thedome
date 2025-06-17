package pl.cuyer.thedome.domain.battlemetrics

import pl.cuyer.thedome.domain.rust.MapPayload
import pl.cuyer.thedome.domain.rust.MapResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/** strips the map-ID out of the thumbnail URL */
fun BattlemetricsServerContent.extractMapId(): String? =
    attributes.details?.rustMaps?.thumbnailUrl
        ?.substringBefore("/thumbnail")
        ?.substringBefore("/Thumbnail")
        ?.substringAfterLast("/")

/** fetch map icon url from RustMaps */
suspend fun HttpClient.fetchMapIcon(mapId: String, apiKey: String): String? =
    try {
        val response: MapResponse = get("https://api.rustmaps.com/v4/maps/$mapId") {
            header("X-API-Key", apiKey)
        }.body()
        response.data.url
    } catch (e: Exception) {
        null
    }
