package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable
import pl.cuyer.thedome.domain.auth.AuthProvider

@Serializable
data class AccessToken(
    val accessToken: String,
    val username: String,
    val provider: AuthProvider
)
