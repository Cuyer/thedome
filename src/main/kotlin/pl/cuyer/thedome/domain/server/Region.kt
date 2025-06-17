package pl.cuyer.thedome.domain.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Region {
    ASIA,
    EUROPE,
    @SerialName("NORTH AMERICA")
    AMERICA,
    AFRICA,
    @SerialName("SOUTH AMERICA")
    SOUTH_AMERICA,
    @SerialName("OCEANIA")
    OCEANIA,
    @SerialName("AUSTRALIA")
    AUSTRALIA
}
