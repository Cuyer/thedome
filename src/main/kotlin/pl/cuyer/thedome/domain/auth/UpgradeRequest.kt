package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class UpgradeRequest(
    val username: String,
    val password: String,
    val email: String
)
