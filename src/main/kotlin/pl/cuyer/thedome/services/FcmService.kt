package pl.cuyer.thedome.services

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.auth.oauth2.GoogleCredentials
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.client.model.Filters
import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.services.NotificationType
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.services.FcmTokenService

class FcmService(
    private val messaging: FirebaseMessaging,
    private val servers: MongoCollection<BattlemetricsServerContent>,
    private val notifyBeforeWipe: List<Int>,
    private val notifyBeforeMapWipe: List<Int>,
    private val credentials: GoogleCredentials,
    private val users: MongoCollection<User>,
    private val tokenService: FcmTokenService
) {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    suspend fun checkAndSend() {
        val now = Clock.System.now()
        data class Range(val start: String, val end: String)

        val wipeRanges = notifyBeforeWipe.map { minutes ->
            val start = now + minutes.minutes - 60.seconds
            val end = now + minutes.minutes
            Range(start.toString(), end.toString())
        }
        val mapRanges = notifyBeforeMapWipe.map { minutes ->
            val start = now + minutes.minutes - 60.seconds
            val end = now + minutes.minutes
            Range(start.toString(), end.toString())
        }

        val filters = (wipeRanges.map { r ->
            Filters.and(
                Filters.exists("attributes.details.rust_next_wipe"),
                Filters.gte("attributes.details.rust_next_wipe", r.start),
                Filters.lt("attributes.details.rust_next_wipe", r.end)
            )
        } + mapRanges.map { r ->
            Filters.and(
                Filters.exists("attributes.details.rust_next_wipe_map"),
                Filters.gte("attributes.details.rust_next_wipe_map", r.start),
                Filters.lt("attributes.details.rust_next_wipe_map", r.end)
            )
        }).toTypedArray()

        if (filters.isEmpty()) return
        val filter = Filters.or(*filters)
        val due = servers.find(filter).toList()

        for (server in due) {
            val id = server.id
            if (id.isEmpty()) continue
            val details = server.attributes.details
            val mapWipe = details?.rustNextWipeMap
            val wipe = details?.rustNextWipe
            val mapDue = mapWipe != null && mapRanges.any { mapWipe >= it.start && mapWipe < it.end }
            val type = if (mapDue) NotificationType.MapWipe else NotificationType.Wipe
            val timestamp = if (mapDue) mapWipe else wipe
            val name = server.attributes.name
            if (timestamp != null && name != null) {
                val subscribers = users.find(Filters.eq("subscriptions", id)).toList()
                for (user in subscribers) {
                    for (token in user.fcmTokens) {
                        sendToToken(user.username, token.token, name, type, timestamp)
                    }
                }
            }
        }
    }

    private suspend fun sendToToken(username: String, token: String, name: String, type: NotificationType, timestamp: String) {
        logger.info("Sending notification to token '{}'", token)
        val message = Message.builder()
            .setToken(token)
            .putData("name", name)
            .putData("type", type.name)
            .putData("timestamp", timestamp)
            .build()
        try {
            credentials.refreshIfExpired()
            messaging.send(message)
        } catch (e: FirebaseMessagingException) {
            if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED || e.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                tokenService.removeToken(username, token)
            }
            logger.warn("FCM error ${e.message}")
        } catch (e: Exception) {
            logger.warn("FCM error ${e.message}")
        }
    }
}
