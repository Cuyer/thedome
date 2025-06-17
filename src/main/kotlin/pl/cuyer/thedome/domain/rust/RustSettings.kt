package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.cuyer.thedome.domain.rust.FlexibleFloatSerializer

@Serializable
data class RustSettings(
    @SerialName("blueprints")
    val blueprints: Boolean? = null,
    @SerialName("decay")
    @Serializable(with = FlexibleFloatSerializer::class)
    val decay: Float? = null,
    @SerialName("forceWipeType")
    val forceWipeType: String? = null,
    @SerialName("groupLimit")
    val groupLimit: Int? = null,
    @SerialName("kits")
    val kits: Boolean? = null,
    @SerialName("rates")
    val rates: Rates? = null,
    @SerialName("teamUILimit")
    val teamUILimit: Int? = null,
    @SerialName("upkeep")
    val upkeep: Double? = null,
    @SerialName("wipes")
    val wipes: List<Wipe> = emptyList(),
    @SerialName("timeZone")
    val timezone: String? = null
)
