package pl.cuyer.thedome.domain.fcm

import kotlinx.serialization.Serializable
import pl.cuyer.thedome.services.NotificationType

@Serializable
data class FcmTopicRequest(
    val topic: String,
    val name: String,
    val type: NotificationType,
    val timestamp: String
)
