package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class RefreshRequest(val refreshToken: String)
