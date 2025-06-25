package pl.cuyer.thedome.services

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
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
    private val notifyBeforeMapWipe: Int
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
            if (id.isNullOrEmpty()) continue
            val name = server.attributes.name ?: "Server $id"
            val nextMap = server.attributes.details?.rustNextWipeMap

            val mapDue = nextMap != null && nextMap >= mapStart.toString() && nextMap < mapEnd.toString()
            val title = if (mapDue) "Server map wipe" else "Server wipe"
            val type = if (mapDue) NotificationType.MapWipe else NotificationType.Wipe
            sendToTopic(id, title, name, type)
        }
    }

    private fun sendToTopic(topic: String, title: String, body: String, type: NotificationType) {
        val message = Message.builder()
            .setTopic(topic)
            .putData("title", title)
            .putData("body", body)
            .putData("id", topic)
            .putData("type", type.name)
            .build()
        try {
            messaging.send(message)
        } catch (e: Exception) {
            logger.warn("FCM error ${e.message}")
        }
    }
}


