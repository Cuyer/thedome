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

class FavouritesServiceTest {
    @Test
    fun `addFavourite pushes server id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favourites = emptyList(), subscriptions = emptyList())
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FavouritesService(users, servers, 3)

        val result = service.addFavourite("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
    }

    @Test
    fun `addFavourite respects limit`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favourites = listOf("1", "2", "3"), subscriptions = emptyList())
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val service = FavouritesService(users, servers, 3)

        val result = service.addFavourite("user", "4")

        assertFalse(result)
    }

    @Test
    fun `addFavourite ignores limit for subscriber`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val slotUpdate = slot<Bson>()
        val user = User(username = "user", email = null, passwordHash = "", favourites = listOf("1", "2", "3"), subscribed = true, subscriptions = emptyList())
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FavouritesService(users, servers, 3)

        val result = service.addFavourite("user", "4")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("4"))
    }

    @Test
    fun `removeFavourite pulls server id`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favourites = listOf("1"), subscriptions = emptyList())
        val slotUpdate = slot<Bson>()
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { users.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = FavouritesService(users, servers, 3)

        val result = service.removeFavourite("user", "1")

        assertTrue(result)
        assertTrue(slotUpdate.captured.toString().contains("1"))
    }

    @Test
    fun `removeFavourite returns false when missing`() = runBlocking {
        val users = mockk<MongoCollection<User>>(relaxed = true)
        val servers = mockk<MongoCollection<BattlemetricsServerContent>>(relaxed = true)
        val user = User(username = "user", email = null, passwordHash = "", favourites = emptyList(), subscriptions = emptyList())
        every { users.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        val service = FavouritesService(users, servers, 3)

        val result = service.removeFavourite("user", "1")

        assertFalse(result)
    }
}
