package pl.cuyer.thedome.services

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineCollection
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.toServerInfo
import pl.cuyer.thedome.domain.server.Order
import pl.cuyer.thedome.domain.server.ServerInfo
import pl.cuyer.thedome.domain.server.ServersResponse
import pl.cuyer.thedome.resources.Servers
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

class ServersService(
    private val collection: CoroutineCollection<BattlemetricsServerContent>
) {
    private val logger = LoggerFactory.getLogger(ServersService::class.java)
    suspend fun getServers(params: Servers, favorites: List<String>? = null): ServersResponse {
        logger.info("Querying servers with params: $params")
        val page = params.page ?: 1
        val size = params.size ?: 20
        val skip = (page - 1) * size

        val filters = mutableListOf<Bson>()
        params.map?.let {
            val pattern = Pattern.compile(it.name.replace('_', ' '), Pattern.CASE_INSENSITIVE)
            filters += Filters.regex("attributes.details.map", pattern)
        }
        params.flag?.let { filters += Filters.eq("attributes.country", it.name) }
        params.region?.let {
            val pattern = Pattern.compile("^${it.name}", Pattern.CASE_INSENSITIVE)
            filters += Filters.regex("attributes.details.rust_settings.timeZone", pattern)
        }
        params.difficulty?.let {
            val pattern = Pattern.compile(it.name, Pattern.CASE_INSENSITIVE)
            filters += Filters.regex("attributes.details.rust_gamemode", pattern)
        }
        params.modded?.let { modded ->
            val pattern = Pattern.compile("modded", Pattern.CASE_INSENSITIVE)
            val regexFilter = Filters.regex("attributes.details.rust_type", pattern)
            filters += if (modded) regexFilter else Filters.not(regexFilter)
        }
        params.official?.let { filters += Filters.eq("attributes.details.official", it) }
        params.ranking?.let { filters += Filters.lte("attributes.rank", it) }
        params.playerCount?.let { filters += Filters.gte("attributes.players", it) }
        params.groupLimit?.let { filters += Filters.eq("attributes.details.rust_settings.groupLimit", it) }
        params.name?.let {
            val pattern = Pattern.compile(".*${Pattern.quote(it)}.*", Pattern.CASE_INSENSITIVE)
            filters += Filters.regex("attributes.name", pattern)
        }

        val sortField = when (params.order) {
            Order.RANK -> "attributes.rank"
            Order.PLAYER_COUNT -> "attributes.players"
            else -> "attributes.details.rust_last_wipe"
        }
        val sort = if (params.order == Order.RANK) Sorts.ascending(sortField) else Sorts.descending(sortField)

        val query = if (filters.isEmpty()) collection.find() else collection.find(Filters.and(filters))

        val totalItems = if (filters.isEmpty()) {
            collection.countDocuments()
        } else {
            collection.countDocuments(Filters.and(filters))
        }

        val serverInfos = query
            .sort(sort)
            .skip(skip)
            .limit(size)
            .toList()
            .map { it.toServerInfo() }
            .filter { params.wipeSchedule == null || it.wipeSchedule == params.wipeSchedule }

        val favoritesList = favorites ?: emptyList()

        val enriched = serverInfos.map { info ->
            val fav = info.id?.toString()?.let { favoritesList.contains(it) } ?: false
            info.copy(isFavorite = fav)
        }

        val totalPages = if (size == 0) 0 else ((totalItems + size - 1) / size).toInt()

        val response = ServersResponse(
            page = page,
            size = size,
            totalPages = totalPages,
            totalItems = totalItems,
            servers = enriched
        )
        logger.info("Returning ${'$'}{serverInfos.size} servers for page $page")
        return response
    }
}
