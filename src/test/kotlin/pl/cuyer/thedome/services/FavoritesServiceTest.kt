package pl.cuyer.thedome.services

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.model.Filters.eq
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import com.mongodb.kotlin.client.coroutine.FindFlow
import pl.cuyer.thedome.util.SimpleFindPublisher

class FavoritesServiceTest {
    @Test
    fun `addFavorite pushes server id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favorites = emptyList())
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FavoritesService(users, servers, 3)

        val result = service.addFavorite("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
    }

    @Test
    fun `addFavorite respects limit`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favorites = listOf("1", "2", "3"))
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val service = FavoritesService(users, servers, 3)

        val result = service.addFavorite("user", "4")

        assertFalse(result)
    }

    @Test
    fun `addFavorite ignores limit for subscriber`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val slotUpdate = slot<Bson>()
        val user = User(username = "user", email = null, passwordHash = "", favorites = listOf("1", "2", "3"), subscriber = true)
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FavoritesService(users, servers, 3)

        val result = service.addFavorite("user", "4")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("4"))
    }

    @Test
    fun `removeFavorite pulls server id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favorites = listOf("1"))
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FavoritesService(users, servers, 3)

        val result = service.removeFavorite("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
    }

    @Test
    fun `removeFavorite returns false when missing`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favorites = emptyList())
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val service = FavoritesService(users, servers, 3)

        val result = service.removeFavorite("user", "1")

        assertFalse(result)
    }
}
