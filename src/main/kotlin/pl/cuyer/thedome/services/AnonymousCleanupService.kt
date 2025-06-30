package pl.cuyer.thedome.services

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.auth.User

class AnonymousCleanupService(
    private val collection: MongoCollection<User>
) {
    private val logger = LoggerFactory.getLogger(AnonymousCleanupService::class.java)

    suspend fun cleanupExpiredUsers() {
        val now = Clock.System.now().toString()
        val filter = Filters.and(
            Filters.regex("username", "^anon-"),
            Filters.lt("testEndsAt", now)
        )
        val result = collection.deleteMany(filter)
        logger.info("Removed ${'$'}{result.deletedCount} expired anonymous users")
    }
}
