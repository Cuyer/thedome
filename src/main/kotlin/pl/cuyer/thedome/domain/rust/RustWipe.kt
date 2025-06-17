package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RustWipe(
    @SerialName("timestamp")
    val timestamp: String,
    @SerialName("type")
    val type: String
)
