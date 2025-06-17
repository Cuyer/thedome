package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Wipe(
    @SerialName("days")
    val days: List<String> = emptyList(),
    @SerialName("hour")
    val hour: Int? = null,
    @SerialName("minute")
    val minute: Int? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("weeks")
    val weeks: List<Int> = emptyList()
)
