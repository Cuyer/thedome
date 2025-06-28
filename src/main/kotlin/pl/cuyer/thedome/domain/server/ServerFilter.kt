package pl.cuyer.thedome.domain.server

import kotlinx.serialization.Serializable

@Serializable
enum class ServerFilter {
    ALL,
    FAVOURITES,
    SUBSCRIBED
}

