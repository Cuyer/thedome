package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class User(
    @SerialName("_id")
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId? = null,
    val username: String,
    val email: String? = null,
    val passwordHash: String,
    val refreshToken: String? = null,
    val testEndsAt: String? = null,
    val subscriber: Boolean = false,
    val favourites: List<String> = emptyList(),
    val subscriptions: List<String> = emptyList(),
    val fcmTokens: List<FcmToken> = emptyList()
)

