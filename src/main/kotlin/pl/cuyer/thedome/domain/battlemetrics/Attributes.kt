package pl.cuyer.thedome.domain.battlemetrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attributes(
    @SerialName("country")
    val country: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    @SerialName("details")
    val details: Details? = null,
    @SerialName("id")
    val id: String,
    @SerialName("ip")
    val ip: String? = null,
    @SerialName("maxPlayers")
    val maxPlayers: Int? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("players")
    val players: Int? = null,
    @SerialName("port")
    val port: Int? = null,
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null
)
