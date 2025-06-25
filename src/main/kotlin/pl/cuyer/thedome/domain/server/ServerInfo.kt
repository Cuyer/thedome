package pl.cuyer.thedome.domain.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class ServerInfo(
    val id: Long? = null,
    val name: String? = null,
    val wipe: Instant? = null,
    @SerialName("next_wipe")
    val nextWipe: Instant? = null,
    @SerialName("next_map_wipe")
    val nextMapWipe: Instant? = null,
    val status: ServerStatus? = null,
    val ranking: Int? = null,
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
    @SerialName("map_url")
    val mapUrl: String? = null,
    @SerialName("header_image")
    val headerImage: String? = null,
    val description: String? = null,
    @SerialName("wipe_type")
    val wipeType: WipeType? = null,
    val blueprints: Boolean? = null,
    val kits: Boolean? = null,
    val decay: Float? = null,
    val upkeep: Double? = null,
    val rates: Int? = null,
    val seed: Long? = null,
    @SerialName("map_size")
    val mapSize: Int? = null,
    @SerialName("entity_count")
    val entityCount: Double? = null,
    val monuments: Int? = null,
    @SerialName("average_fps")
    val averageFps: Long? = null,
    val pve: Boolean? = null,
    val website: String? = null,
    @SerialName("is_premium")
    val isPremium: Boolean? = null,
    @SerialName("is_favorite")
    val isFavorite: Boolean = false,
    @SerialName("is_subscribed")
    val isSubscribed: Boolean = false
)

