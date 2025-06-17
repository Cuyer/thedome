package pl.cuyer.thedome.domain.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServersResponse(
    val page: Int,
    val size: Int,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_items")
    val totalItems: Long,
    val servers: List<ServerInfo>
)
