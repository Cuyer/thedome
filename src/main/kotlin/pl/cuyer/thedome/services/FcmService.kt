package pl.cuyer.thedome.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    private data class Range(val start: Instant, val end: Instant)

    suspend fun checkAndSend() = coroutineScope {
        val now = Clock.System.now()

        val wipeRanges = buildRanges(now, notifyBeforeWipe)
        val mapRanges = buildRanges(now, notifyBeforeMapWipe)

        val filter = buildCombinedFilter(wipeRanges, mapRanges) ?: return@coroutineScope

        val dueServers = servers.find(filter).toList()

        dueServers.map { server ->
            async {
                notifySubscribersIfDue(server, wipeRanges, mapRanges)
            }
        }.awaitAll()
    }

    private fun buildRanges(now: Instant, minutesList: List<Int>): List<Range> {
        return minutesList.map {
            val end = now + it.minutes
            val start = end - 60.seconds
            Range(start, end)
        }
    }

    private fun buildCombinedFilter(
        wipeRanges: List<Range>,
        mapRanges: List<Range>
    ): Bson? {
        val filters = buildRangeFilters(wipeRanges, "attributes.details.rust_next_wipe") +
            buildRangeFilters(mapRanges, "attributes.details.rust_next_wipe_map")

        return if (filters.isNotEmpty()) Filters.or(filters) else null
    }

    private fun buildRangeFilters(ranges: List<Range>, field: String): List<Bson> {
        return ranges.map {
            Filters.and(
                Filters.exists(field),
                Filters.gte(field, it.start.toString()),
                Filters.lt(field, it.end.toString())
            )
        }
    }

    private suspend fun notifySubscribersIfDue(
        server: BattlemetricsServerContent,
        wipeRanges: List<Range>,
        mapRanges: List<Range>
    ) {
        val id = server.id.takeIf { it.isNotEmpty() } ?: return
        val details = server.attributes.details ?: return
        val name = server.attributes.name ?: return

        val mapWipe = details.rustNextWipeMap
        val wipe = details.rustNextWipe

        val isMapDue = mapWipe != null && mapRanges.any { it.contains(mapWipe) }
        val isWipeDue = wipe != null && wipeRanges.any { it.contains(wipe) }

        val type = when {
            isMapDue -> NotificationType.MapWipe
            isWipeDue -> NotificationType.Wipe
            else -> return
        }

        val timestamp = if (isMapDue) mapWipe else wipe ?: return

        val hasSubscribers = users.find(Filters.eq("subscriptions", id))
            .limit(1)
            .toList()
            .isNotEmpty()

        if (hasSubscribers) {
            sendToTopic(id, name, type, timestamp)
        }
    }

    private fun Range.contains(value: String): Boolean {
        return try {
            val instant = Instant.parse(value)
            instant >= start && instant < end
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendToTopic(topic: String, name: String, type: NotificationType, timestamp: String) {
        logger.info("Sending notification to topic '{}'", topic)

        val message = Message.builder()
            .setTopic(topic)
            .putData("name", name)
            .putData("type", type.name)
            .putData("timestamp", timestamp)
            .build()

        runCatching {
            credentials.refreshIfExpired()
            withContext(Dispatchers.IO) {
                messaging.sendAsync(message).get()
            }
        }.onFailure { e ->
            when (e) {
                is FirebaseMessagingException -> logger.warn("FCM error: ${e.message}")
                else -> logger.warn("Unexpected FCM error: ${e.message}")
            }
        }
    }
}
