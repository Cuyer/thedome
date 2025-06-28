package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val username: String,
    val email: String?
)
