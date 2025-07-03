package pl.cuyer.thedome.services

import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.model.Filters.eq
import com.mongodb.kotlin.client.model.Updates.push
import com.mongodb.kotlin.client.model.Updates.pull
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.firstOrNull
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.exceptions.SubscriptionLimitException

class SubscriptionsService(
    private val users: MongoCollection<User>,
    private val limit: Int,
    private val messaging: FirebaseMessaging
) {
    suspend fun getSubscriptions(username: String): List<String> {
        val user = users.find(eq(User::username, username)).firstOrNull()
        return user?.subscriptions ?: emptyList()
    }

    suspend fun subscribe(username: String, serverId: String): Boolean {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return false
        if (user.subscriptions.contains(serverId)) return true
        if (!user.subscribed && user.subscriptions.size >= limit) throw SubscriptionLimitException()
        users.updateOne(eq(User::username, username), push(User::subscriptions, serverId))
        if (user.fcmTokens.isNotEmpty()) {
            try {
                messaging.subscribeToTopic(user.fcmTokens.map { it.token }, serverId)
            } catch (_: Exception) {}
        }
        return true
    }

    suspend fun unsubscribe(username: String, serverId: String): Boolean {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return false
        if (!user.subscriptions.contains(serverId)) return false
        users.updateOne(eq(User::username, username), pull(User::subscriptions, serverId))
        if (user.fcmTokens.isNotEmpty()) {
            try {
                messaging.unsubscribeFromTopic(user.fcmTokens.map { it.token }, serverId)
            } catch (_: Exception) {}
        }
        return true
    }
}
