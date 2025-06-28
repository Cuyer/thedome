package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class AccessToken(val accessToken: String, val username: String)
