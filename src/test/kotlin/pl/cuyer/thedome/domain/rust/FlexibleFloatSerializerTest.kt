import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pl.cuyer.thedome.domain.rust.FlexibleFloatSerializer

class FlexibleFloatSerializerTest {
    private val json = Json

    @Test
    fun `deserialize handles numeric string`() {
        val value = json.decodeFromString(FlexibleFloatSerializer, "\"1.5\"")
        assertEquals(1.5f, value)
    }

    @Test
    fun `deserialize handles number`() {
        val value = json.decodeFromString(FlexibleFloatSerializer, "2.5")
        assertEquals(2.5f, value)
    }

    @Test
    fun `deserialize handles boolean`() {
        val value = json.decodeFromString(FlexibleFloatSerializer, "true")
        assertEquals(1f, value)
    }

    @Test
    fun `deserialize returns null for invalid string`() {
        val value = json.decodeFromString(FlexibleFloatSerializer, "\"abc\"")
        assertNull(value)
    }

    @Test
    fun `serialize encodes float`() {
        val encoded = json.encodeToString(FlexibleFloatSerializer, 3.5f)
        assertEquals("3.5", encoded)
    }
}
