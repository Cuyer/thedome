import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineFindPublisher
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.battlemetrics.*
import pl.cuyer.thedome.domain.server.*
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServersService

class ServersServiceTest {
    @Test
    fun `getServers filters by name`() = runBlocking {
        val attr1 = Attributes(id = "a1", name = "Cool Server")
        val server1 = BattlemetricsServerContent(attributes = attr1, id = "1")

        val publisher = mockk<CoroutineFindPublisher<BattlemetricsServerContent>>()
        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns publisher
        every { publisher.sort(any<Bson>()) } returns publisher
        every { publisher.skip(any()) } returns publisher
        every { publisher.limit(any()) } returns publisher
        coEvery { publisher.toList() } returns listOf(server1)
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(name = "cool"))

        assertEquals(1, response.totalItems)
        assertEquals("Cool Server", response.servers.first().name)
    }

    @Test
    fun `getServers filters by region`() = runBlocking {
        val attr = Attributes(id = "a1", name = "Region Server")
        val server = BattlemetricsServerContent(attributes = attr, id = "1")

        val publisher = mockk<CoroutineFindPublisher<BattlemetricsServerContent>>()
        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns publisher
        every { publisher.sort(any<Bson>()) } returns publisher
        every { publisher.skip(any()) } returns publisher
        every { publisher.limit(any()) } returns publisher
        coEvery { publisher.toList() } returns listOf(server)
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(region = Region.EUROPE))

        assertEquals(1, response.totalItems)
        assertEquals("Region Server", response.servers.first().name)
        val filter = slotFind.captured.toString()
        assertTrue(filter.contains("rust_settings.timeZone"))
        assertTrue(filter.contains("EUROPE", ignoreCase = true))
    }

    @Test
    fun `getServers filters by difficulty`() = runBlocking {
        val attr = Attributes(id = "a2", name = "Difficulty Server")
        val server = BattlemetricsServerContent(attributes = attr, id = "2")

        val publisher = mockk<CoroutineFindPublisher<BattlemetricsServerContent>>()
        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns publisher
        every { publisher.sort(any<Bson>()) } returns publisher
        every { publisher.skip(any()) } returns publisher
        every { publisher.limit(any()) } returns publisher
        coEvery { publisher.toList() } returns listOf(server)
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(difficulty = Difficulty.VANILLA))

        assertEquals(1, response.totalItems)
        assertEquals("Difficulty Server", response.servers.first().name)
        val filter = slotFind.captured.toString()
        assertTrue(filter.contains("rust_gamemode"))
        assertTrue(filter.contains("VANILLA"))
    }

    @Test
    fun `getServers filters by ranking and player count`() = runBlocking {
        val attr = Attributes(id = "a3", name = "Ranking Server")
        val server = BattlemetricsServerContent(attributes = attr, id = "3")

        val publisher = mockk<CoroutineFindPublisher<BattlemetricsServerContent>>()
        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns publisher
        every { publisher.sort(any<Bson>()) } returns publisher
        every { publisher.skip(any()) } returns publisher
        every { publisher.limit(any()) } returns publisher
        coEvery { publisher.toList() } returns listOf(server)
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(ranking = 50, playerCount = 10))

        assertEquals(1, response.totalItems)
        assertEquals("Ranking Server", response.servers.first().name)
        val filter = slotFind.captured.toString()
        assertTrue(filter.contains("attributes.rank"))
        assertTrue(filter.contains("50"))
        assertTrue(filter.contains("attributes.players"))
        assertTrue(filter.contains("10"))
    }

    @Test
    fun `getServers filters by modded and official`() = runBlocking {
        val attr = Attributes(id = "a4", name = "Modded Official Server")
        val server = BattlemetricsServerContent(attributes = attr, id = "4")

        val publisher = mockk<CoroutineFindPublisher<BattlemetricsServerContent>>()
        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns publisher
        every { publisher.sort(any<Bson>()) } returns publisher
        every { publisher.skip(any()) } returns publisher
        every { publisher.limit(any()) } returns publisher
        coEvery { publisher.toList() } returns listOf(server)
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(modded = true, official = true))

        assertEquals(1, response.totalItems)
        assertEquals("Modded Official Server", response.servers.first().name)
        val filter = slotFind.captured.toString()
        assertTrue(filter.contains("rust_type"))
        assertTrue(filter.contains("modded", ignoreCase = true))
        assertTrue(filter.contains("official"))
    }

    @Test
    fun `getServers marks favorites`() = runBlocking {
        val attr = Attributes(id = "a5", name = "Fav Server")
        val server = BattlemetricsServerContent(attributes = attr, id = "5")

        val publisher = mockk<CoroutineFindPublisher<BattlemetricsServerContent>>()
        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns publisher
        every { publisher.sort(any<Bson>()) } returns publisher
        every { publisher.skip(any()) } returns publisher
        every { publisher.limit(any()) } returns publisher
        coEvery { publisher.toList() } returns listOf(server)
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(), listOf("5"))

        assertTrue(response.servers.first().isFavorite)
    }
}
