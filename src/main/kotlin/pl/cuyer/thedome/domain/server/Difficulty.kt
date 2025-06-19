package pl.cuyer.thedome.domain.server

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty {
    VANILLA, SOFTCORE, HARDCORE, PRIMITIVE
}
