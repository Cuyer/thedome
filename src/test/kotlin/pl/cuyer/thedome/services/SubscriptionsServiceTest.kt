package pl.cuyer.thedome.services

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.model.Filters.eq
import com.google.firebase.messaging.FirebaseMessaging
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.auth.FcmToken
import pl.cuyer.thedome.util.SimpleFindPublisher
import pl.cuyer.thedome.exceptions.SubscriptionLimitException

class SubscriptionsServiceTest {
    @Test
    fun `subscribe pushes id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = emptyList(), fcmTokens = listOf(FcmToken("token1", "ts")))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = SubscriptionsService(users, 3, messaging)

        val result = service.subscribe("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
        verify { messaging.subscribeToTopic(listOf("token1"), "1") }
    }

    @Test
    fun `subscribe respects limit`() = runBlocking<Unit> {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = listOf("1", "2", "3"))
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val service = SubscriptionsService(users, 3, messaging)
        assertFailsWith<SubscriptionLimitException> {
            service.subscribe("user", "4")
        }
    }

    @Test
    fun `subscribe ignores limit for subscriber`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val slotUpdate = slot<Bson>()
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = listOf("1", "2", "3"), subscribed = true)
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val service = SubscriptionsService(users, 3, messaging)

        val result = service.subscribe("user", "4")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("4"))
    }

    @Test
    fun `unsubscribe pulls id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = listOf("1"), fcmTokens = listOf(FcmToken("token1", "ts")))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val service = SubscriptionsService(users, 3, messaging)

        val result = service.unsubscribe("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
        verify { messaging.unsubscribeFromTopic(listOf("token1"), "1") }
    }

    @Test
    fun `unsubscribe returns false when missing`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = emptyList())
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val service = SubscriptionsService(users, 3, messaging)

        val result = service.unsubscribe("user", "1")

        assertFalse(result)
    }
}

