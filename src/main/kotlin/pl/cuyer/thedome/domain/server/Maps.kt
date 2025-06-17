package pl.cuyer.thedome.domain.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Maps {
    CUSTOM,
    PROCEDURAL,
    BARREN,
    @SerialName("CRAGGY ISLAND")
    CRAGGY_ISLAND,
    @SerialName("HAPPIS ISLAND")
    HAPPIS_ISLAND,
    @SerialName("SAVAS ISLAND KOTH")
    SAVAS_ISLAND_KOTH,
    @SerialName("SAVAS ISLAND")
    SAVAS_ISLAND
}
