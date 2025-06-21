package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId

@Serializable
data class User(
    @BsonId val id: String? = null,
    val username: String,
    val passwordHash: String,
    val refreshToken: String? = null
)
