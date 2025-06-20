import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineFindPublisher
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.battlemetrics.*
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
}
