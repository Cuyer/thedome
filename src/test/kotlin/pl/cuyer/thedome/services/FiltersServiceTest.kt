import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineFindPublisher
import pl.cuyer.thedome.domain.battlemetrics.*
import pl.cuyer.thedome.domain.rust.RustMaps
import pl.cuyer.thedome.domain.rust.RustSettings
import pl.cuyer.thedome.domain.rust.Wipe
import pl.cuyer.thedome.domain.server.*
import pl.cuyer.thedome.services.FiltersService

class FiltersServiceTest {
    @Test
    fun `getOptions aggregates distinct values`() = runBlocking {
        val settings1 = RustSettings(groupLimit = 5, wipes = listOf(Wipe(weeks = listOf(1))), timezone = "Europe/London")
        val rustMaps1 = RustMaps(thumbnailUrl = null, imageIconUrl = null)
        val details1 = Details(
            map = "Procedural Map",
            rustGamemode = "vanilla",
            rustSettings = settings1,
            rustMaps = rustMaps1
        )
        val attr1 = Attributes(country = "GB", details = details1, id = "1", players = 10, rank = 3)
        val server1 = BattlemetricsServerContent(attributes = attr1, id = "1")

        val settings2 = RustSettings(groupLimit = 8, wipes = listOf(Wipe(weeks = List(10) { 1 })), timezone = "America/New_York")
        val rustMaps2 = RustMaps(thumbnailUrl = null, imageIconUrl = null)
        val details2 = Details(
            map = "Barren",
            rustGamemode = "softcore",
            rustSettings = settings2,
            rustMaps = rustMaps2
        )
        val attr2 = Attributes(country = "US", details = details2, id = "2", players = 50, rank = 1)
        val server2 = BattlemetricsServerContent(attributes = attr2, id = "2")

        val publisher = mockk<CoroutineFindPublisher<BattlemetricsServerContent>>()
        val collection = mockk<CoroutineCollection<BattlemetricsServerContent>>()
        every { collection.find() } returns publisher
        coEvery { publisher.toList() } returns listOf(server1, server2)

        val service = FiltersService(collection)
        val options = service.getOptions()

        assertEquals(listOf(Flag.GB, Flag.US), options.flags)
        assertEquals(3, options.maxRanking)
        assertEquals(50, options.maxPlayerCount)
        assertEquals(8, options.maxGroupLimit)
        assertEquals(listOf(Maps.BARREN, Maps.PROCEDURAL), options.maps)
        assertEquals(listOf(Region.AMERICA, Region.EUROPE), options.regions)
        assertEquals(listOf(Difficulty.SOFTCORE, Difficulty.VANILLA), options.difficulty)
        assertEquals(listOf(WipeSchedule.MONTHLY, WipeSchedule.WEEKLY), options.wipeSchedules)
    }
}
