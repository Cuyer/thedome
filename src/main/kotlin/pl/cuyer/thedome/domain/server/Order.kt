package pl.cuyer.thedome.domain.server

import kotlinx.serialization.Serializable

@Serializable
enum class Order {
    WIPE,
    RANK,
    PLAYER_COUNT
}
