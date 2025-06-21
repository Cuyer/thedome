package pl.cuyer.thedome.services

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Sorts
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bson.BsonDocument
import org.litote.kmongo.coroutine.CoroutineCollection
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.server.*

class FiltersService(private val collection: CoroutineCollection<BattlemetricsServerContent>) {
    suspend fun getOptions(): FiltersOptions = coroutineScope {
        val flags = async {
            val docs = collection.aggregate<BsonDocument>(
                listOf(
                    Aggregates.match(Filters.exists("attributes.country")),
                    Aggregates.group("\$attributes.country")
                )
            ).toList()
            docs.mapNotNull { doc ->
                val value = doc.getString("_id")?.value
                try { value?.let { Flag.valueOf(it) } } catch (_: Exception) { null }
            }.distinct().sortedBy { it.name }
        }

        val maxRanking = async {
            collection
                .find()
                .sort(Sorts.descending("attributes.rank"))
                .limit(1)
                .first()
                ?.attributes?.rank ?: 0
        }

        val maxPlayerCount = async {
            collection
                .find()
                .sort(Sorts.descending("attributes.players"))
                .limit(1)
                .first()
                ?.attributes?.players ?: 0
        }

        val maxGroupLimit = async {
            collection
                .find()
                .sort(Sorts.descending("attributes.details.rust_settings.groupLimit"))
                .limit(1)
                .first()
                ?.attributes?.details?.rustSettings?.groupLimit ?: 0
        }

        val maps = async {
            val docs = collection.aggregate<BsonDocument>(
                listOf(
                    Aggregates.match(Filters.exists("attributes.details.map")),
                    Aggregates.group("\$attributes.details.map")
                )
            ).toList()
            docs.mapNotNull { doc ->
                val raw = doc.getString("_id")?.value ?: return@mapNotNull null
                val key = raw.substringBefore(" ").uppercase()
                try { Maps.valueOf(key) } catch (_: Exception) { Maps.CUSTOM }
            }.distinct().sortedBy { it.name }
        }

        val regions = async {
            val docs = collection.aggregate<BsonDocument>(
                listOf(
                    Aggregates.match(Filters.exists("attributes.details.rust_settings.timeZone")),
                    Aggregates.group("\$attributes.details.rust_settings.timeZone")
                )
            ).toList()
            docs.mapNotNull { doc ->
                val raw = doc.getString("_id")?.value ?: return@mapNotNull null
                val key = raw.substringBefore("/").uppercase()
                try { Region.valueOf(key) } catch (_: Exception) { null }
            }.distinct().sortedBy { it.name }
        }

        val difficulty = async {
            val docs = collection.aggregate<BsonDocument>(
                listOf(
                    Aggregates.match(Filters.exists("attributes.details.rust_gamemode")),
                    Aggregates.group("\$attributes.details.rust_gamemode")
                )
            ).toList()
            docs.mapNotNull { doc ->
                val raw = doc.getString("_id")?.value ?: return@mapNotNull null
                try { Difficulty.valueOf(raw.uppercase()) } catch (_: Exception) { null }
            }.distinct().sortedBy { it.name }
        }

        val wipeSchedules = async {
            val docs = collection
                .find()
                .projection(
                    Projections.fields(
                        Projections.include(
                            "id",
                            "attributes.id",
                            "attributes.details.rust_settings.wipes"
                        ),
                        Projections.excludeId()
                    )
                )
                .toList()
            docs.mapNotNull { doc ->
                doc.attributes.details?.rustSettings?.wipes?.let { WipeSchedule.from(it) }
            }.distinct().sortedBy { it.name }
        }
        FiltersOptions(
            flags = flags.await(),
            maxRanking = maxRanking.await(),
            maxPlayerCount = maxPlayerCount.await(),
            maxGroupLimit = maxGroupLimit.await(),
            maps = maps.await(),
            regions = regions.await(),
            difficulty = difficulty.await(),
            wipeSchedules = wipeSchedules.await()
        )
    }
}
