package pl.cuyer.thedome.services

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.auth.oauth2.GoogleCredentials
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.coEvery
import io.mockk.coVerify
import pl.cuyer.thedome.domain.auth.FcmToken
import pl.cuyer.thedome.services.FcmTokenService
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.battlemetrics.Attributes
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.Details
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.util.SimpleFindPublisher
import kotlin.test.Test
import kotlin.test.assertEquals

class FcmServiceTest {
    @Test
    fun `checkAndSend sends message using firebase`() = runBlocking {
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val credentials = mockk<GoogleCredentials>(relaxed = true)
        val tokenService = mockk<FcmTokenService>(relaxed = true)
        val now = Clock.System.now()
        val server = BattlemetricsServerContent(
            attributes = Attributes(
                id = "a1",
                name = "Test Server",
                details = Details(rustNextWipe = (now + 30.seconds).toString())
            ),
            id = "1"
        )
        val serverColl = mockk<MongoCollection<BattlemetricsServerContent>>()
        every { serverColl.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server)))
        val usersColl = mockk<MongoCollection<User>>()
        val user = User(username = "user", passwordHash = "", subscriptions = listOf("1"), fcmTokens = listOf(FcmToken("t1", "ts")))
        every { usersColl.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))

        val service = FcmService(messaging, serverColl, listOf(1), emptyList(), credentials, usersColl, tokenService)
        service.checkAndSend()

        val captured = slot<Message>()
        verify { credentials.refreshIfExpired() }
        verify { messaging.send(capture(captured)) }
        val message = captured.captured
        fun field(target: Any, name: String): Any? = target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target)
        assertEquals("1", field(message, "topic"))
        assertEquals(null, field(message, "notification"))
        val data = field(message, "data") as Map<*, *>
        assertEquals("Test Server", data["name"])
        assertEquals("Wipe", data["type"])
        assertEquals((now + 30.seconds).toString(), data["timestamp"])
    }

    @Test
    fun `send failure does not remove token`() = runBlocking {
        val exception = mockk<FirebaseMessagingException>()
        every { exception.message } returns "error"
        val messaging = mockk<FirebaseMessaging>()
        every { messaging.send(any()) } throws exception
        val credentials = mockk<GoogleCredentials>(relaxed = true)
        val tokenService = mockk<FcmTokenService>(relaxed = true)
        val now = Clock.System.now()
        val server = BattlemetricsServerContent(
            attributes = Attributes(
                id = "a1",
                name = "Test Server",
                details = Details(rustNextWipe = (now + 30.seconds).toString())
            ),
            id = "1"
        )
        val serverColl = mockk<MongoCollection<BattlemetricsServerContent>>()
        every { serverColl.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server)))
        val usersColl = mockk<MongoCollection<User>>()
        val user = User(username = "user", passwordHash = "", subscriptions = listOf("1"), fcmTokens = listOf(FcmToken("t1", "ts")))
        every { usersColl.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))

        val service = FcmService(messaging, serverColl, listOf(1), emptyList(), credentials, usersColl, tokenService)
        service.checkAndSend()

        coVerify(exactly = 0) { tokenService.removeToken(any(), any()) }
    }
}
