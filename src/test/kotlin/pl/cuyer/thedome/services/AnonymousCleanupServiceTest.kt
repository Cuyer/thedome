package pl.cuyer.thedome.services

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.auth.User
import com.mongodb.client.result.DeleteResult

class AnonymousCleanupServiceTest {
    @Test
    fun `cleanupExpiredUsers removes anonymous`() = runBlocking {
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        val filterSlot = slot<Bson>()
        val result = mockk<DeleteResult> {
            every { deletedCount } returns 2
        }
        coEvery { collection.deleteMany(capture(filterSlot), any()) } returns result

        val service = AnonymousCleanupService(collection)
        service.cleanupExpiredUsers()

        coVerify { collection.deleteMany(any<Bson>(), any()) }
        val filterString = filterSlot.captured.toString()
        assertTrue(filterString.contains("username") && filterString.contains("testEndsAt"))
    }
}
