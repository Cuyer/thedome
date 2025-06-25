package pl.cuyer.thedome.services

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.auth.oauth2.GoogleCredentials
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.battlemetrics.Attributes
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.Details
import pl.cuyer.thedome.util.SimpleFindPublisher
import kotlin.test.Test
import kotlin.test.assertEquals

class FcmServiceTest {
    @Test
    fun `checkAndSend sends message using firebase`() = runBlocking {
        val messaging = mockk<FirebaseMessaging>(relaxed = true)
        val credentials = mockk<GoogleCredentials>(relaxed = true)
        val now = Clock.System.now()
        val server = BattlemetricsServerContent(
            attributes = Attributes(
                id = "a1",
                name = "Test Server",
                details = Details(rustNextWipe = (now + 30.seconds).toString())
            ),
            id = "1"
        )
        val collection = mockk<MongoCollection<BattlemetricsServerContent>>()
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(server)))

        val service = FcmService(messaging, collection, 1, 0, credentials)
        service.checkAndSend()

        val captured = slot<Message>()
        verify { credentials.refreshIfExpired() }
        verify { messaging.sendAsync(capture(captured)) }
        val message = captured.captured
        fun field(target: Any, name: String): Any? = target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target)
        assertEquals("1", field(message, "topic"))
        assertEquals(null, field(message, "notification"))
        val data = field(message, "data") as Map<*, *>
        assertEquals("1", data["id"])
        assertEquals("Wipe", data["type"])
    }
}
