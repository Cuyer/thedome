package pl.cuyer.thedome.domain.rust

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

/** Serializer that accepts numeric values encoded as either numbers or strings. */
object FlexibleFloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleFloat", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: Float) {
        encoder.encodeFloat(value)
    }

    override fun deserialize(decoder: Decoder): Float {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This class can be loaded only by JSON")
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonPrimitive) {
            val content = element.content
            return content.toFloatOrNull()
                ?: throw SerializationException("Expected a float but got '$content'")
        } else {
            throw SerializationException("Expected JsonPrimitive")
        }
    }
}
