package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

/** Serializer that accepts numeric values encoded as either numbers or strings. */
object FlexibleFloatSerializer : KSerializer<Float?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleFloat", PrimitiveKind.FLOAT)

    override fun deserialize(decoder: Decoder): Float? {
        return when (val element = (decoder as? JsonDecoder)?.decodeJsonElement()) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content.toFloatOrNull()
                    element.booleanOrNull != null -> if (element.boolean) 1f else 0f
                    element.intOrNull != null -> element.int.toFloat()
                    element.floatOrNull != null -> element.floatOrNull
                    element.doubleOrNull != null -> element.double.toFloat()
                    else -> null
                }
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Float?) {
        encoder.encodeFloat(value ?: 0f)
    }
}