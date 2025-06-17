package pl.cuyer.thedome.domain.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class ServerInfo(
    val id: Long? = null,
    val name: String? = null,
    val wipe: Instant? = null,
    val ranking: Double? = null,
    val modded: Boolean? = null,
    @SerialName("player_count")
    val playerCount: Long? = null,
    @SerialName("server_capacity")
    val serverCapacity: Long? = null,
    @SerialName("map_name")
    val mapName: Maps? = null,
    val cycle: Double? = null,
    @SerialName("server_flag")
    val serverFlag: Flag? = null,
    val region: Region? = null,
    @SerialName("max_group")
    val maxGroup: Long? = null,
    val difficulty: Difficulty? = null,
    @SerialName("wipe_schedule")
    val wipeSchedule: WipeSchedule? = null,
    val isOfficial: Boolean? = null,
    val serverIp: String? = null,
    @SerialName("map_image")
    val mapImage: String? = null,
    val description: String? = null,
    val mapId: String? = null
)
