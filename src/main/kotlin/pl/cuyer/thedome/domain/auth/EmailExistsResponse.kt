package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class EmailExistsResponse(
    val exists: Boolean,
    val provider: AuthProvider? = null
)
