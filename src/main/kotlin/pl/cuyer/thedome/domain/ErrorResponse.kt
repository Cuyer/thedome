package pl.cuyer.thedome.domain

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String,
    val cause: String? = null
)
