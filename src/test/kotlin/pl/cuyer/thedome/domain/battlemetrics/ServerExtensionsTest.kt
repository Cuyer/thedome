package pl.cuyer.thedome.domain.battlemetrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pl.cuyer.thedome.domain.rust.RustMaps

class ServerExtensionsTest {
    private fun serverWithUrl(url: String?): BattlemetricsServerContent {
        val rustMaps = if (url != null) RustMaps(thumbnailUrl = url) else null
        val details = Details(rustMaps = rustMaps)
        val attributes = Attributes(id = "1", details = details)
        return BattlemetricsServerContent(attributes = attributes, id = "1")
    }

    @Test
    fun `extractMapId handles lowercase path`() {
        val server = serverWithUrl("https://example.com/maps/abc123/thumbnail.png")
        assertEquals("abc123", server.extractMapId())
    }

    @Test
    fun `extractMapId handles capital Thumbnail`() {
        val server = serverWithUrl("https://example.com/maps/XYZ/Thumbnail.jpg")
        assertEquals("XYZ", server.extractMapId())
    }

    @Test
    fun `extractMapId returns null without url`() {
        val server = serverWithUrl(null)
        assertNull(server.extractMapId())
    }
}
