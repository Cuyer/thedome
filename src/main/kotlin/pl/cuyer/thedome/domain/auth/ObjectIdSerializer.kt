package pl.cuyer.thedome.domain.auth

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.ExperimentalSerializationApi
import org.bson.BsonObjectId
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder
import org.bson.types.ObjectId

@OptIn(ExperimentalSerializationApi::class)
object ObjectIdSerializer : KSerializer<ObjectId> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ObjectId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ObjectId) {
        when (encoder) {
            is BsonEncoder -> encoder.encodeBsonValue(BsonObjectId(value)) // BSON serialization
            else -> encoder.encodeString(value.toHexString()) // JSON serialization
        }
    }

    override fun deserialize(decoder: Decoder): ObjectId {
        return when (decoder) {
            is BsonDecoder -> decoder.decodeBsonValue().asObjectId().value // BSON deserialization
            else -> ObjectId(decoder.decodeString()) // JSON deserialization
        }
    }
}
