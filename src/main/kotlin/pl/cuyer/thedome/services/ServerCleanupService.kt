package pl.cuyer.thedome.services

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import kotlin.time.Duration.Companion.days

class ServerCleanupService(
    private val collection: MongoCollection<BattlemetricsServerContent>
) {
    private val logger = LoggerFactory.getLogger(ServerCleanupService::class.java)

    suspend fun cleanupOldServers() {
        val cutoff: Instant = Clock.System.now() - 60.days
        val result = collection.deleteMany(Filters.lt("attributes.updatedAt", cutoff.toString()))
        logger.info("Removed ${result.deletedCount} outdated servers")
    }
}
