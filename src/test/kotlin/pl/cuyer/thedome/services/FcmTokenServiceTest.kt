package pl.cuyer.thedome.services

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.auth.FcmToken
import pl.cuyer.thedome.util.SimpleFindPublisher

class FcmTokenServiceTest {
    @Test
    fun `registerToken adds token with timestamp and subscribes`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = listOf("s1"))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FcmTokenService(users, messaging)

        service.registerToken("user", "token1", "ts")

        assertTrue(slotUpdate.captured.toString().contains("token1"))
        verify { messaging.subscribeToTopic(listOf("token1"), "s1") }
    }

    @Test
    fun `registerToken removes invalid token on subscribe failure`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val exception = mockk<FirebaseMessagingException>()
        every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
        every { messaging.subscribeToTopic(listOf("token1"), "s1") } throws exception
        val user = User(username = "user", email = null, passwordHash = "", subscriptions = listOf("s1"))
        val updates = mutableListOf<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(updates), any()) } returns mockk()
        val service = FcmTokenService(users, messaging)

        service.registerToken("user", "token1", "ts")

        assertTrue(updates.size == 2)
        assertTrue(!updates.last().toString().contains("token1"))
        verify { messaging.unsubscribeFromTopic(listOf("token1"), "s1") }
    }

    @Test
    fun `removeToken removes token`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", fcmTokens = listOf(FcmToken("token1", "ts")), subscriptions = listOf("s1"))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FcmTokenService(users, messaging)

        service.removeToken("user", "token1")

        verify { messaging.unsubscribeFromTopic(listOf("token1"), "s1") }
        assertTrue(slotUpdate.captured.toString().contains("fcmTokens"))
    }

    @Test
    fun `removeToken still removes when unsubscribe fails`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val messaging = mockk<FirebaseMessaging>()
        val exception = mockk<FirebaseMessagingException>()
        every { exception.messagingErrorCode } returns MessagingErrorCode.INVALID_ARGUMENT
        every { messaging.unsubscribeFromTopic(listOf("token1"), "s1") } throws exception
        val user = User(username = "user", email = null, passwordHash = "", fcmTokens = listOf(FcmToken("token1", "ts")), subscriptions = listOf("s1"))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FcmTokenService(users, messaging)

        service.removeToken("user", "token1")

        verify { messaging.unsubscribeFromTopic(listOf("token1"), "s1") }
        assertTrue(slotUpdate.captured.toString().contains("fcmTokens"))
    }

    @Test
    fun `removeStaleTokens removes old tokens and unsubscribes`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val old = FcmToken("t1", (Clock.System.now() - 40.days).toString())
        val fresh = FcmToken("t2", Clock.System.now().toString())
        val user = User(username = "user", passwordHash = "", fcmTokens = listOf(old, fresh), subscriptions = listOf("s1"))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FcmTokenService(users, messaging)

        service.removeStaleTokens(30)

        assertTrue(slotUpdate.captured.toString().contains("t2"))
        verify { messaging.unsubscribeFromTopic(listOf("t1"), "s1") }
    }

    @Test
    fun `resubscribeTokens subscribes tokens again`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val user = User(
            username = "user",
            passwordHash = "",
            fcmTokens = listOf(FcmToken("t1", "ts")),
            subscriptions = listOf("s1")
        )
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val service = FcmTokenService(users, messaging)

        service.resubscribeTokens()

        verify { messaging.subscribeToTopic(listOf("t1"), "s1") }
    }
}

