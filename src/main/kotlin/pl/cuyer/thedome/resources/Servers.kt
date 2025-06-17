package pl.cuyer.thedome.resources

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.cuyer.thedome.domain.server.Difficulty
import pl.cuyer.thedome.domain.server.Flag
import pl.cuyer.thedome.domain.server.Maps
import pl.cuyer.thedome.domain.server.Region
import pl.cuyer.thedome.domain.server.WipeSchedule
import pl.cuyer.thedome.domain.server.Order

@Serializable
@Resource("/servers")
data class Servers(
    val page: Int? = null,
    val size: Int? = null,
    val map: Maps? = null,
    val flag: Flag? = null,
    val region: Region? = null,
    val difficulty: Difficulty? = null,
    val modded: Boolean? = null,
    val official: Boolean? = null,
    val wipeSchedule: WipeSchedule? = null,
    val rank: Int? = null,
    @SerialName("player_count")
    val playerCount: Int? = null,
    @SerialName("server_capacity")
    val serverCapacity: Int? = null,
    val order: Order? = null
)
