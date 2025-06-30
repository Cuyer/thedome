package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleTokenInfo(
    @SerialName("aud") val audience: String,
    @SerialName("sub") val subject: String,
    @SerialName("name") val name: String? = null,
    @SerialName("givenName") val givenName: String? = null,
    val email: String? = null
)

