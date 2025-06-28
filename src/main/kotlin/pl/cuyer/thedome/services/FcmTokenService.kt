package pl.cuyer.thedome.services

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.model.Filters.eq
import com.mongodb.kotlin.client.model.Filters.ne
import com.mongodb.kotlin.client.model.Updates.set
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.days
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import pl.cuyer.thedome.domain.auth.FcmToken
import pl.cuyer.thedome.domain.auth.User

class FcmTokenService(
    private val users: MongoCollection<User>,
    private val messaging: FirebaseMessaging
) {
    suspend fun registerToken(username: String, token: String) {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return
        val exists = user.fcmTokens.any { it.token == token }
        val newTokens = user.fcmTokens.filter { it.token != token } +
            FcmToken(token, Clock.System.now().toString())
        users.updateOne(eq(User::username, username), set(User::fcmTokens, newTokens))
        if (!exists && user.subscriptions.isNotEmpty()) {
            for (topic in user.subscriptions) {
                try {
                    messaging.subscribeToTopic(listOf(token), topic)
                } catch (e: Exception) {
                    if (e is FirebaseMessagingException &&
                        (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                            e.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT)) {
                        removeToken(username, token)
                    }
                }
            }
        }
    }

    suspend fun removeToken(username: String, token: String) {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return
        val newTokens = user.fcmTokens.filter { it.token != token }
        users.updateOne(eq(User::username, username), set(User::fcmTokens, newTokens))
        if (user.subscriptions.isNotEmpty()) {
            for (topic in user.subscriptions) {
                try {
                    messaging.unsubscribeFromTopic(listOf(token), topic)
                } catch (e: Exception) {
                    if (e is FirebaseMessagingException &&
                        (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                            e.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT)) {
                        // token already invalid, ignore
                    }
                }
            }
        }
    }

    suspend fun removeStaleTokens(days: Int = 30) {
        val cutoff = Clock.System.now() - days.days
        val candidates = users.find(ne(User::fcmTokens, emptyList<FcmToken>())).toList()
        for (user in candidates) {
            val stale = user.fcmTokens.filter { Instant.parse(it.updatedAt) < cutoff }
            if (stale.isNotEmpty()) {
                val remaining = user.fcmTokens.filter { Instant.parse(it.updatedAt) >= cutoff }
                users.updateOne(eq(User::username, user.username), set(User::fcmTokens, remaining))
                if (user.subscriptions.isNotEmpty()) {
                    for (topic in user.subscriptions) {
                        try {
                            messaging.unsubscribeFromTopic(stale.map { it.token }, topic)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    suspend fun resubscribeTokens() {
        val candidates = users.find(ne(User::fcmTokens, emptyList<FcmToken>())).toList()
        for (user in candidates) {
            if (user.subscriptions.isEmpty()) continue
            val tokens = user.fcmTokens.map { it.token }
            for (topic in user.subscriptions) {
                try {
                    messaging.subscribeToTopic(tokens, topic)
                } catch (_: Exception) {}
            }
        }
    }
}

