package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable

@Serializable
enum class AuthProvider {
    LOCAL,
    GOOGLE,
    ANONYMOUS
}
