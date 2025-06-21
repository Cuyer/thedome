package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RustMaps(
    @SerialName("barren")
    val barren: Boolean? = null,
    @SerialName("biomePercentages")
    val biomePercentages: BiomePercentages? = null,
    @SerialName("iceLakes")
    val iceLakes: Int? = null,
    @SerialName("islands")
    val islands: Int? = null,
    @SerialName("mapUrl")
    val mapUrl: String? = null,
    @SerialName("monumentCount")
    val monumentCount: Int? = null,
    @SerialName("monumentCounts")
    val monumentCounts: MonumentCounts? = null,
    @SerialName("mountains")
    val mountains: Int? = null,
    @SerialName("rivers")
    val rivers: Int? = null,
    @SerialName("seed")
    val seed: Long? = null,
    @SerialName("size")
    val size: Int? = null,
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    @SerialName("imageIconUrl")
    val imageIconUrl: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("url")
    val url: String? = null
)
