package pl.cuyer.thedome.services

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.auth.oauth2.GoogleCredentials
import pl.cuyer.thedome.services.NotificationType
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.toList
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.client.model.Filters
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import org.slf4j.LoggerFactory

class FcmService(
    private val messaging: FirebaseMessaging,
    private val servers: MongoCollection<BattlemetricsServerContent>,
    private val notifyBeforeWipe: Int,
    private val notifyBeforeMapWipe: Int,
    private val credentials: GoogleCredentials
) {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    suspend fun checkAndSend() {
        val now = Clock.System.now()

        val wipeStart = now + notifyBeforeWipe.minutes - 60.seconds
        val wipeEnd = now + notifyBeforeWipe.minutes
        val mapStart = now + notifyBeforeMapWipe.minutes - 60.seconds
        val mapEnd = now + notifyBeforeMapWipe.minutes

        val filter = Filters.or(
            Filters.and(
                Filters.exists("attributes.details.rust_next_wipe"),
                Filters.gte("attributes.details.rust_next_wipe", wipeStart.toString()),
                Filters.lt("attributes.details.rust_next_wipe", wipeEnd.toString())
            ),
            Filters.and(
                Filters.exists("attributes.details.rust_next_wipe_map"),
                Filters.gte("attributes.details.rust_next_wipe_map", mapStart.toString()),
                Filters.lt("attributes.details.rust_next_wipe_map", mapEnd.toString())
            )
        )

        val due = servers.find(filter).toList()
        for (server in due) {
            val id = server.id
            if (id.isEmpty()) continue
            val nextMap = server.attributes.details?.rustNextWipeMap
            val mapDue = nextMap != null && nextMap >= mapStart.toString() && nextMap < mapEnd.toString()
            val type = if (mapDue) NotificationType.MapWipe else NotificationType.Wipe
            sendToTopic(id, type)
        }
    }

    private fun sendToTopic(topic: String, type: NotificationType) {
        logger.info("Sending notification to topic '{}'", topic)
        val message = Message.builder()
            .setTopic(topic)
            .putData("id", topic)
            .putData("type", type.name)
            .build()
        try {
            credentials.refreshIfExpired()
            messaging.sendAsync(message)
        } catch (e: Exception) {
            logger.warn("FCM error ${e.message}")
        }
    }
}


