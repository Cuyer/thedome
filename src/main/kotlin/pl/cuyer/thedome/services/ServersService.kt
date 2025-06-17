package pl.cuyer.thedome.services

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineCollection
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.toServerInfo
import pl.cuyer.thedome.domain.server.Order
import pl.cuyer.thedome.domain.server.ServerInfo
import pl.cuyer.thedome.resources.Servers
import java.util.regex.Pattern

class ServersService(private val collection: CoroutineCollection<BattlemetricsServerContent>) {
    suspend fun getServers(params: Servers): List<ServerInfo> {
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
            filters += Filters.regex("attributes.details.rustSettings.timezone", pattern)
        }
        params.difficulty?.let {
            val pattern = Pattern.compile(it.name, Pattern.CASE_INSENSITIVE)
            filters += Filters.regex("attributes.details.rustGamemode", pattern)
        }
        params.modded?.let { modded ->
            val pattern = Pattern.compile("modded", Pattern.CASE_INSENSITIVE)
            val regexFilter = Filters.regex("attributes.details.rustType", pattern)
            filters += if (modded) regexFilter else Filters.not(regexFilter)
        }
        params.official?.let { filters += Filters.eq("attributes.details.official", it) }
        params.rank?.let { filters += Filters.eq("attributes.rank", it) }
        params.playerCount?.let { filters += Filters.eq("attributes.players", it) }
        params.serverCapacity?.let { filters += Filters.eq("attributes.maxPlayers", it) }

        val sortField = when (params.order) {
            Order.RANK -> "attributes.rank"
            Order.PLAYER_COUNT -> "attributes.players"
            else -> "attributes.details.rustLastWipe"
        }
        val sort = if (params.order == Order.RANK) Sorts.ascending(sortField) else Sorts.descending(sortField)

        val query = if (filters.isEmpty()) collection.find() else collection.find(Filters.and(filters))

        val serverInfos = query
            .sort(sort)
            .skip(skip)
            .limit(size)
            .toList()
            .map { it.toServerInfo() }

        return serverInfos.filter { params.wipeSchedule == null || it.wipeSchedule == params.wipeSchedule }
    }
}
