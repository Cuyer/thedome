package pl.cuyer.thedome.services

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.bson.conversions.Bson
import com.mongodb.kotlin.client.coroutine.MongoCollection
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import kotlin.test.Test
import kotlin.test.assertTrue

class ServerCleanupServiceTest {
    @Test
    fun `cleanupOldServers removes outdated`() = runBlocking {
        val collection = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val filterSlot = slot<Bson>()
        val result = mockk<com.mongodb.client.result.DeleteResult> {
            every { deletedCount } returns 5
        }
        coEvery { collection.deleteMany(capture(filterSlot), any()) } returns result

        val service = ServerCleanupService(collection)
        service.cleanupOldServers()

        coVerify(exactly = 1) { collection.deleteMany(any<Bson>(), any()) }
        assertTrue(filterSlot.captured.toString().contains("attributes.updatedAt"))
    }
}
