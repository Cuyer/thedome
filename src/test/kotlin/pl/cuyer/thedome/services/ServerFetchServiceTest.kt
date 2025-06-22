package pl.cuyer.thedome.services

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.DeleteOptions
import com.mongodb.client.model.ReplaceOneModel
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineCollection
import pl.cuyer.thedome.domain.battlemetrics.*

class ServerFetchServiceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `fetchServers upserts and removes`() = runBlocking {
        val server1 = BattlemetricsServerContent(attributes = Attributes(id = "a1"), id = "1")
        val server2 = BattlemetricsServerContent(attributes = Attributes(id = "a2"), id = "2")
        val page = BattlemetricsPage(data = listOf(server1, server2), links = Links(null))

        val engine = MockEngine { request ->
            respond(
                content = ByteReadChannel(json.encodeToString(page)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }

        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        val slotOps = slot<List<ReplaceOneModel<BattlemetricsServerContent>>>()
        val slotFilter = slot<Bson>()
        coEvery { collection.bulkWrite(capture(slotOps), any<BulkWriteOptions>()) } returns mockk()
        coEvery { collection.deleteMany(capture(slotFilter)) } returns mockk()

        val service = ServerFetchService(client, collection, "")
        service.fetchServers()

        assertEquals(2, slotOps.captured.size)
        val ids = slotOps.captured.map { it.replacement.id }.toSet()
        assertEquals(setOf("1", "2"), ids)
        coVerify(exactly = 1) { collection.deleteMany(any<Bson>(), any<DeleteOptions>()) }
    }
}
