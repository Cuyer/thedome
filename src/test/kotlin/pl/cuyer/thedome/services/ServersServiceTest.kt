import io.mockk.*
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.battlemetrics.*
import pl.cuyer.thedome.domain.server.*
import pl.cuyer.thedome.resources.Servers
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.util.SimpleFindPublisher

class ServersServiceTest {
    @Test
    fun `getServers filters by name`() = runBlocking {
        val attr1 = Attributes(id = "a1", name = "Cool Server")
        val server1 = BattlemetricsServerContent(attributes = attr1, id = "1")

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server1)))
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

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns FindFlow(SimpleFindPublisher(listOf(server)))
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

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns FindFlow(SimpleFindPublisher(listOf(server)))
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

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns FindFlow(SimpleFindPublisher(listOf(server)))
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

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val slotFind = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns FindFlow(SimpleFindPublisher(listOf(server)))
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

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server)))
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(), listOf("5"))

        assertTrue(response.servers.first().isFavorite)
    }

    @Test
    fun `getServers marks subscriptions`() = runBlocking {
        val attr = Attributes(id = "a6", name = "Sub Server")
        val server = BattlemetricsServerContent(attributes = attr, id = "6")

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server)))
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(Servers(), subscriptions = listOf("6"))

        assertTrue(response.servers.first().isSubscribed)
    }

    @Test
    fun `getServers filters favorites`() = runBlocking {
        val attr = Attributes(id = "a7", name = "Fav Only")
        val server = BattlemetricsServerContent(attributes = attr, id = "7")

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server)))
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(
            Servers(filter = ServerFilter.FAVORITES),
            favorites = listOf("7")
        )

        assertEquals(1, response.servers.size)
        assertTrue(response.servers.first().isFavorite)
    }

    @Test
    fun `getServers filters subscriptions`() = runBlocking {
        val attr = Attributes(id = "a8", name = "Sub Only")
        val server = BattlemetricsServerContent(attributes = attr, id = "8")

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server)))
        coEvery { collection.countDocuments(any<Bson>()) } returns 1
        coEvery { collection.countDocuments(any<Bson>(), any()) } returns 1

        val service = ServersService(collection)
        val response = service.getServers(
            Servers(filter = ServerFilter.SUBSCRIBED),
            subscriptions = listOf("8")
        )

        assertEquals(1, response.servers.size)
        assertTrue(response.servers.first().isSubscribed)
    }
}


