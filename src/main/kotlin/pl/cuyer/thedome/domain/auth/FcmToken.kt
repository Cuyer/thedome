package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class FcmToken(
    val token: String,
    val updatedAt: String
)
