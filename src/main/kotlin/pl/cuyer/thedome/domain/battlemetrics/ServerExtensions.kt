package pl.cuyer.thedome.domain.battlemetrics

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pl.cuyer.thedome.domain.rust.MapPayload
import pl.cuyer.thedome.domain.rust.MapResponse
import pl.cuyer.thedome.domain.rust.RustWipe
import pl.cuyer.thedome.domain.server.*
import kotlinx.datetime.Instant

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

fun BattlemetricsServerContent.toServerInfo(): ServerInfo =
    ServerInfo(
        id = id.toLongOrNull(),
        name = attributes.name,
        wipe = attributes.details?.rustLastWipe?.let(Instant::parse),
        status = attributes.status?.uppercase()?.let {
            try {
                ServerStatus.valueOf(it)
            } catch (e: Exception) {
                null
            }
        },
        ranking = attributes.rank,
        modded = attributes.details?.rustType?.let {
            it.contains("modded", ignoreCase = true) || it.contains("community", ignoreCase = true)
        },
        playerCount = attributes.players?.toLong(),
        serverCapacity = attributes.maxPlayers?.toLong(),
        mapName = attributes.details?.map?.substringBefore(" ")?.uppercase()?.let {
            try {
                Maps.valueOf(it)
            } catch (e: Exception) {
                Maps.CUSTOM
            }
        },
        cycle = attributes.details?.rustWipes?.let { calculateCycle(it) },
        serverFlag = attributes.country?.let { Flag.valueOf(it) },
        region = attributes.details?.rustSettings?.timezone?.substringBefore("/")?.uppercase()
            ?.let {
                try {
                    Region.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            },
        maxGroup = attributes.details?.rustSettings?.groupLimit?.toLong(),
        difficulty = attributes.details?.rustGamemode?.uppercase()?.let {
            try {
                Difficulty.valueOf(it)
            } catch (e: Exception) {
                null
            }
        },
        wipeSchedule = attributes.details?.rustSettings?.wipes?.let { WipeSchedule.from(it) },
        isOfficial = attributes.details?.official,
        serverIp = ipPort(attributes.ip ?: "", attributes.port?.toString() ?: ""),
        mapImage = attributes.details?.rustMaps?.imageIconUrl,
        description = attributes.details?.rustDescription,
        wipeType = attributes.details?.rustWipes?.firstOrNull()?.type?.uppercase()?.let {
            try {
                WipeType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        },
    )

private fun calculateCycle(wipes: List<RustWipe>): Double? {
    val instants = wipes
        .map { Instant.parse(it.timestamp) }
        .sorted()

    if (instants.size < 2) return null

    val intervals = instants
        .zipWithNext()
        .map { (earlier, later) -> later - earlier }

    val avgInterval = intervals
        .reduce { sum, d -> sum + d } / intervals.size

    return avgInterval.inWholeDays.toDouble()
}

private fun ipPort(ip: String, port: String): String = "$ip:$port"
