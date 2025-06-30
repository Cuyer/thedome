package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable
import pl.cuyer.thedome.domain.auth.AuthProvider

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val username: String,
    val email: String?,
    val provider: AuthProvider
)
