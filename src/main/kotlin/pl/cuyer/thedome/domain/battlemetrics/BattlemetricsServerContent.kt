package pl.cuyer.thedome.domain.battlemetrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BattlemetricsServerContent(
    @SerialName("attributes")
    val attributes: Attributes,
    @SerialName("id")
    val id: String
)
