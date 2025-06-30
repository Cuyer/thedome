package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(val token: String)

