package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapResponse(
    @SerialName("data") val data: MapPayload
)

@Serializable
data class MapPayload(
    @SerialName("imageIconUrl") val url: String
)
