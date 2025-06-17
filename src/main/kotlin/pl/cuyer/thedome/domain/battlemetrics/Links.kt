package pl.cuyer.thedome.domain.battlemetrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Links(
    @SerialName("next")
    val next: String? = null
)
