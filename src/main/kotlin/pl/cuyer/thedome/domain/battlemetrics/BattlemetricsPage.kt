package pl.cuyer.thedome.domain.battlemetrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BattlemetricsPage(
    @SerialName("data")
    val data: List<BattlemetricsServerContent> = emptyList(),
    @SerialName("links")
    val links: Links? = null
)
