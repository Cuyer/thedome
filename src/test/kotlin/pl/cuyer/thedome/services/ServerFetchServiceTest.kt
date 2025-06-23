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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.DeleteOptions
import com.mongodb.client.model.ReplaceOneModel
import org.bson.conversions.Bson
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import kotlinx.coroutines.flow.toList
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

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>()
        val findPub = mockk<FindFlow<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns findPub
        coEvery { findPub.toList() } returns emptyList()
        val slotOps = slot<List<ReplaceOneModel<BattlemetricsServerContent>>>()
        val slotFilter = slot<Bson>()
        coEvery { collection.bulkWrite(capture(slotOps), any<BulkWriteOptions>()) } returns mockk()
        coEvery { collection.deleteMany(capture(slotFilter), any<DeleteOptions>()) } returns mockk()

        val service = ServerFetchService(client, collection, "")
        service.fetchServers()

        assertEquals(2, slotOps.captured.size)
        val ids = slotOps.captured.map { it.replacement.id }.toSet()
        assertEquals(setOf("1", "2"), ids)
        coVerify(exactly = 1) { collection.deleteMany(any<Bson>(), any<DeleteOptions>()) }
    }

    @Test
    fun `fetchServers only updates newer data`() = runBlocking {
        val year = Clock.System.now().toLocalDateTime(TimeZone.UTC).year
        val new1 = BattlemetricsServerContent(
            attributes = Attributes(id = "a1", updatedAt = "${year}-01-02T00:00:00Z"),
            id = "1"
        )
        val new2 = BattlemetricsServerContent(
            attributes = Attributes(id = "a2", updatedAt = "${year}-01-01T00:00:00Z"),
            id = "2"
        )
        val page = BattlemetricsPage(data = listOf(new1, new2), links = Links(null))

        val engine = MockEngine {
            respond(
                content = ByteReadChannel(json.encodeToString(page)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>()
        val findPub = mockk<FindFlow<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns findPub
        val existing1 = BattlemetricsServerContent(
            attributes = Attributes(id = "a1", updatedAt = "${year}-01-01T00:00:00Z"),
            id = "1"
        )
        val existing2 = BattlemetricsServerContent(
            attributes = Attributes(id = "a2", updatedAt = "${year}-01-02T00:00:00Z"),
            id = "2"
        )
        coEvery { findPub.toList() } returns listOf(existing1, existing2)

        val slotOps = slot<List<ReplaceOneModel<BattlemetricsServerContent>>>()
        coEvery { collection.bulkWrite(capture(slotOps), any<BulkWriteOptions>()) } returns mockk()
        coEvery { collection.deleteMany(any<Bson>(), any<DeleteOptions>()) } returns mockk()

        val service = ServerFetchService(client, collection, "")
        service.fetchServers()

        assertEquals(1, slotOps.captured.size)
        assertEquals("1", slotOps.captured.first().replacement.id)
    }

    @Test
    fun `fetchServers filters outdated servers`() = runBlocking {
        val year = Clock.System.now().toLocalDateTime(TimeZone.UTC).year
        val oldYear = year - 1
        val recent = BattlemetricsServerContent(
            attributes = Attributes(id = "a1", updatedAt = "${year}-01-01T00:00:00Z"),
            id = "1"
        )
        val outdated = BattlemetricsServerContent(
            attributes = Attributes(id = "a2", updatedAt = "${oldYear}-12-31T23:59:59Z"),
            id = "2"
        )
        val page = BattlemetricsPage(data = listOf(recent, outdated), links = Links(null))

        val engine = MockEngine {
            respond(
                content = ByteReadChannel(json.encodeToString(page)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }

        val collection = mockk<MongoCollection<BattlemetricsServerContent>>()
        val findPub = mockk<FindFlow<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns findPub
        coEvery { findPub.toList() } returns emptyList()

        val slotOps = slot<List<ReplaceOneModel<BattlemetricsServerContent>>>()
        coEvery { collection.bulkWrite(capture(slotOps), any<BulkWriteOptions>()) } returns mockk()
        coEvery { collection.deleteMany(any<Bson>(), any<DeleteOptions>()) } returns mockk()

        val service = ServerFetchService(client, collection, "")
        service.fetchServers()

        assertEquals(1, slotOps.captured.size)
        assertEquals("1", slotOps.captured.first().replacement.id)
    }
}
