package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Rates(
    @SerialName("component")
    val component: Float? = null,
    @SerialName("craft")
    val craft: Float? = null,
    @SerialName("gather")
    val gather: Float? = null,
    @SerialName("scrap")
    val scrap: Float? = null
)
