package pl.cuyer.thedome.services

import org.litote.kmongo.coroutine.CoroutineCollection
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.toServerInfo
import pl.cuyer.thedome.domain.server.*

class FiltersService(private val collection: CoroutineCollection<BattlemetricsServerContent>) {
    suspend fun getOptions(): FiltersOptions {
        val servers = collection.find().toList().map { it.toServerInfo() }

        val flags = servers.mapNotNull { it.serverFlag }.distinct().sortedBy { it.name }
        val maxRanking = servers.mapNotNull { it.ranking }.maxOrNull() ?: 0
        val maxPlayerCount = servers.mapNotNull { it.playerCount?.toInt() }.maxOrNull() ?: 0
        val maxGroupLimit = servers.mapNotNull { it.maxGroup?.toInt() }.maxOrNull() ?: 0
        val maps = servers.mapNotNull { it.mapName }.distinct().sortedBy { it.name }
        val regions = servers.mapNotNull { it.region }.distinct().sortedBy { it.name }
        val difficulty = servers.mapNotNull { it.difficulty }.distinct().sortedBy { it.name }
        val wipeSchedules = servers.mapNotNull { it.wipeSchedule }.distinct().sortedBy { it.name }

        return FiltersOptions(
            flags = flags,
            maxRanking = maxRanking,
            maxPlayerCount = maxPlayerCount,
            maxGroupLimit = maxGroupLimit,
            maps = maps,
            regions = regions,
            difficulty = difficulty,
            wipeSchedules = wipeSchedules
        )
    }
}
