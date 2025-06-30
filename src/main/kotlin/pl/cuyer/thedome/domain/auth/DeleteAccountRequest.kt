package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class DeleteAccountRequest(
    val username: String,
    val password: String
)
