package pl.cuyer.thedome.domain.fcm

import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenRequest(
    val token: String,
    val timestamp: String
)

