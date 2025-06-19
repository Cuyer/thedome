package pl.cuyer.thedome.domain.battlemetrics

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pl.cuyer.thedome.domain.rust.*
import pl.cuyer.thedome.domain.server.*

class ServerExtensionsAdditionalTest {
    @Test
    fun `fetchMapIcon returns url on success`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals("https://api.rustmaps.com/v4/maps/123", request.url.toString())
            assertEquals("key", request.headers["X-API-Key"])
            respond(
                content = ByteReadChannel("""{"data":{"imageIconUrl":"icon"}}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
        val url = client.fetchMapIcon("123", "key")
        assertEquals("icon", url)
    }

    @Test
    fun `fetchMapIcon returns null on failure`() = runBlocking {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        val url = client.fetchMapIcon("id", "key")
        assertNull(url)
    }

    @Test
    fun `toServerInfo maps all fields`() {
        val wipes = listOf(
            RustWipe(timestamp = "2024-01-01T00:00:00Z", type = "map"),
            RustWipe(timestamp = "2024-01-04T00:00:00Z", type = "map")
        )
        val settings = RustSettings(timezone = "Europe/London", groupLimit = 5, wipes = listOf(Wipe(weeks = listOf(1))))
        val rustMaps = RustMaps(thumbnailUrl = "https://example.com/maps/abc/thumbnail.png", imageIconUrl = "icon.png")
        val details = Details(
            map = "Procedural Map",
            rustLastWipe = "2024-01-01T00:00:00Z",
            rustType = "modded",
            rustGamemode = "vanilla",
            rustSettings = settings,
            rustMaps = rustMaps,
            rustWipes = wipes,
            official = true
        )
        val attributes = Attributes(
            id = "1",
            name = "Test",
            ip = "1.1.1.1",
            port = 28015,
            status = "online",
            players = 10,
            maxPlayers = 50,
            rank = 1,
            country = "GB",
            details = details
        )
        val server = BattlemetricsServerContent(attributes = attributes, id = "1")
        val info = server.toServerInfo()
        assertEquals(1L, info.id)
        assertEquals("Test", info.name)
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), info.wipe)
        assertEquals(1, info.ranking)
        assertEquals(true, info.modded)
        assertEquals(10L, info.playerCount)
        assertEquals(50L, info.serverCapacity)
        assertEquals(Maps.PROCEDURAL, info.mapName)
        assertEquals(3.0, info.cycle)
        assertEquals(Flag.GB, info.serverFlag)
        assertEquals(Region.EUROPE, info.region)
        assertEquals(5L, info.maxGroup)
        assertEquals(Difficulty.VANILLA, info.difficulty)
        assertEquals(WipeSchedule.MONTHLY, info.wipeSchedule)
        assertEquals(true, info.isOfficial)
        assertEquals("1.1.1.1:28015", info.serverIp)
        assertEquals("icon.png", info.mapImage)
        assertEquals(ServerStatus.ONLINE, info.status)
        assertEquals(WipeType.MAP, info.wipeType)
    }

    @Test
    fun `community rust type is considered modded`() {
        val details = Details(rustType = "community")
        val attributes = Attributes(id = "1", details = details)
        val server = BattlemetricsServerContent(attributes = attributes, id = "1")
        val info = server.toServerInfo()
        assertEquals(true, info.modded)
    }
}
