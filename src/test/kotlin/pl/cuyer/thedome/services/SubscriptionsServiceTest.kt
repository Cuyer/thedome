package pl.cuyer.thedome.services

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.model.Filters.eq
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.util.SimpleFindPublisher

class SubscriptionsServiceTest {
    @Test
    fun `subscribe pushes id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = emptyList())
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = SubscriptionsService(users)

        val result = service.subscribe("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
    }

    @Test
    fun `unsubscribe pulls id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = listOf("1"))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = SubscriptionsService(users)

        val result = service.unsubscribe("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
    }

    @Test
    fun `unsubscribe returns false when missing`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = emptyList())
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val service = SubscriptionsService(users)

        val result = service.unsubscribe("user", "1")

        assertFalse(result)
    }
}

