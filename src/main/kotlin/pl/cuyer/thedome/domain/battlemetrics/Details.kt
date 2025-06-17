package pl.cuyer.thedome.domain.battlemetrics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.cuyer.thedome.domain.rust.RustMaps
import pl.cuyer.thedome.domain.rust.RustSettings
import pl.cuyer.thedome.domain.rust.RustWipe

@Serializable
data class Details(
    @SerialName("map")
    val map: String? = null,
    @SerialName("official")
    val official: Boolean? = null,
    @SerialName("pve")
    val pve: Boolean? = null,
    @SerialName("rust_description")
    val rustDescription: String? = null,
    @SerialName("rust_fps_avg")
    val rustFpsAvg: Double? = null,
    @SerialName("rust_gamemode")
    val rustGamemode: String? = null,
    @SerialName("rust_headerimage")
    val rustHeaderimage: String? = null,
    @SerialName("rust_last_wipe")
    val rustLastWipe: String? = null,
    @SerialName("rust_maps")
    val rustMaps: RustMaps? = null,
    @SerialName("rust_type")
    val rustType: String? = null,
    @SerialName("rust_next_wipe")
    val rustNextWipe: String? = null,
    @SerialName("rust_next_wipe_map")
    val rustNextWipeMap: String? = null,
    @SerialName("rust_premium")
    val rustPremium: Boolean? = null,
    @SerialName("rust_queued_players")
    val rustQueuedPlayers: Int? = null,
    @SerialName("rust_settings")
    val rustSettings: RustSettings? = null,
    @SerialName("rust_url")
    val rustUrl: String? = null,
    @SerialName("rust_wipes")
    val rustWipes: List<RustWipe> = emptyList(),
    @SerialName("rust_world_seed")
    val rustWorldSeed: Long? = null,
    @SerialName("rust_world_size")
    val rustWorldSize: Int? = null
)
