package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BiomePercentages(
    @SerialName("d")
    val d: Double? = null,
    @SerialName("f")
    val f: Double? = null,
    @SerialName("j")
    val j: Double? = null,
    @SerialName("s")
    val s: Double? = null,
    @SerialName("t")
    val t: Double? = null
)
