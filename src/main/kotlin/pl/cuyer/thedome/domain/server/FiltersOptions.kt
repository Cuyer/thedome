package pl.cuyer.thedome.domain.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FiltersOptions(
    val flags: List<Flag>,
    @SerialName("max_ranking")
    val maxRanking: Int,
    @SerialName("max_player_count")
    val maxPlayerCount: Int,
    @SerialName("max_group_limit")
    val maxGroupLimit: Int,
    val maps: List<Maps>,
    val regions: List<Region>,
    val difficulty: List<Difficulty>,
    @SerialName("wipe_schedules")
    val wipeSchedules: List<WipeSchedule>
)
