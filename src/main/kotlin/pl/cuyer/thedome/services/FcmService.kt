package pl.cuyer.thedome.services

import com.google.auth.oauth2.GoogleCredentials
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.FileInputStream
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.toList
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.client.model.Filters
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import org.slf4j.LoggerFactory

class FcmService(
    private val httpClient: HttpClient,
    private val servers: MongoCollection<BattlemetricsServerContent>,
    private val json: Json,
    private val notifyBeforeWipe: Int,
    private val notifyBeforeMapWipe: Int
) {
    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    private var cachedToken: Pair<String, Long>? = null

    private suspend fun accessToken(): String {
        val cache = cachedToken
        val now = System.currentTimeMillis()
        if (cache != null && now < cache.second) return cache.first
        val path = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
            ?: throw IllegalStateException("GOOGLE_APPLICATION_CREDENTIALS not set")
        val creds = FileInputStream(path).use { fis ->
            GoogleCredentials.fromStream(fis)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        }
        creds.refresh()
        val token = creds.accessToken.tokenValue
        val expiry = creds.accessToken.expirationTime.time
        cachedToken = token to expiry
        return token
    }

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
            val next = server.attributes.details?.rustNextWipe
            val nextMap = server.attributes.details?.rustNextWipeMap

            val mapDue = nextMap != null && nextMap >= mapStart.toString() && nextMap < mapEnd.toString()
            val title = if (mapDue) "Server map wipe" else "Server wipe"
            sendToTopic(id, title, name)
        }
    }

    private suspend fun sendToTopic(topic: String, title: String, body: String) {
        val token = accessToken()
        val payload = FcmRequest(
            message = Message(
                topic = topic,
                notification = Notification(title, body)
            )
        )
        val response: HttpResponse = httpClient.post("https://fcm.googleapis.com/v1/projects/-/messages:send") {
            header("Authorization", "Bearer $token")
            header("Content-Type", "application/json; UTF-8")
            setBody(json.encodeToString(payload))
        }
        if (response.status.value !in 200..299) {
            logger.warn("FCM error ${response.status.value}")
        }
    }
}

@Serializable
private data class FcmRequest(val message: Message)

@Serializable
private data class Message(val topic: String, val notification: Notification)

@Serializable
private data class Notification(val title: String, val body: String)

