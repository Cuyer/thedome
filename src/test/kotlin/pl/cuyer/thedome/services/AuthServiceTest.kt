package pl.cuyer.thedome.services

import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import com.mongodb.kotlin.client.coroutine.MongoCollection
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.auth.User
import com.mongodb.client.model.InsertOneOptions
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import io.mockk.slot
import com.mongodb.kotlin.client.coroutine.FindFlow
import pl.cuyer.thedome.util.SimpleFindPublisher
import pl.cuyer.thedome.domain.auth.FcmToken
import pl.cuyer.thedome.services.FcmTokenService

class AuthServiceTest {

    private fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    @Test
    fun `registerAnonymous stores user`() = runBlocking {
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        coEvery { collection.insertOne(any(), any<InsertOneOptions>()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience", 3600_000, 3600_000, mockk(relaxed = true))

        val result = service.registerAnonymous()

        assertTrue(result.accessToken.isNotEmpty())
        assertTrue(result.username.startsWith("anon-"))
        coVerify { collection.insertOne(match { it.username.startsWith("anon-") && it.refreshToken == null && it.email == null }, any<InsertOneOptions>()) }
    }
    @Test
    fun `upgradeAnonymous converts user`() = runBlocking {
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        val anon = User(username = "anon-123", email = null, passwordHash = "", refreshToken = null, favourites = emptyList(), subscriptions = emptyList())
        val updated = anon.copy(username = "newuser")
        every { collection.find(any<Bson>()) } returnsMany listOf(
            FindFlow(SimpleFindPublisher(listOf(anon))),
            FindFlow(SimpleFindPublisher(emptyList())),
            FindFlow(SimpleFindPublisher(listOf(updated)))
        )
        coEvery { collection.updateOne(any<Bson>(), any<Bson>(), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience", 3600_000, 3600_000, mockk(relaxed = true))
        val result = service.upgradeAnonymous("anon-123", "newuser", "pass")

        assertTrue(result?.accessToken?.isNotEmpty() == true)
        coVerify(exactly = 3) { collection.updateOne(any<Bson>(), any<Bson>(), any()) }
    }

    @Test
    fun `login hashes refresh token`() = runBlocking {
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        val passwordHash = BCrypt.hashpw("pass", BCrypt.gensalt())
        val user = User(username = "user", email = "user@example.com", passwordHash = passwordHash, favourites = emptyList(), subscriptions = emptyList())
        val slotUpdate = slot<Bson>()
        every { collection.find(any<Bson>()) } returnsMany listOf(
            FindFlow(SimpleFindPublisher(listOf(user))),
            FindFlow(SimpleFindPublisher(listOf(user)))
        )
        coEvery { collection.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val tokenService = mockk<FcmTokenService>(relaxed = true)
        val service = AuthService(collection, "secret", "issuer", "audience", 3600_000, 3600_000, tokenService)

        val result = service.login("user", "pass")

        assertTrue(result?.refreshToken?.isNotEmpty() == true)
        assertTrue(result?.username == "user")
        assertTrue(result?.email == "user@example.com")
        val expected = hash(result!!.refreshToken)
        assertTrue(slotUpdate.captured.toString().contains(expected))
        coVerify { tokenService.resubscribeUserTokens("user") }
    }

    @Test
    fun `refresh compares hashed token`() = runBlocking {
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        val oldToken = "old-token"
        val oldHash = hash(oldToken)
        val user = User(username = "user", email = "user@example.com", passwordHash = "", refreshToken = oldHash, favourites = emptyList(), subscriptions = emptyList())
        val slotFind = slot<Bson>()
        val slotUpdate = slot<Bson>()
        every { collection.find(capture(slotFind)) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { collection.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience", 3600_000, 3600_000, mockk(relaxed = true))

        val result = service.refresh(oldToken)

        assertTrue(result?.refreshToken?.isNotEmpty() == true)
        assertTrue(result?.username == "user")
        assertTrue(result?.email == "user@example.com")
        val expectedFind = slotFind.captured.toString()
        assertTrue(expectedFind.contains(oldHash))
        val expectedUpdate = hash(result!!.refreshToken)
        assertTrue(slotUpdate.captured.toString().contains(expectedUpdate))
    }

    @Test
    fun `logout clears refresh and all tokens`() = runBlocking {
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        val tokenService = mockk<FcmTokenService>(relaxed = true)
        val user = User(
            username = "user",
            passwordHash = "",
            refreshToken = "hash",
            fcmTokens = listOf(FcmToken("t1", "ts"), FcmToken("t2", "ts"))
        )
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { collection.updateOne(any<Bson>(), any<Bson>(), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience", 3600_000, 3600_000, tokenService)

        val result = service.logout("user")

        assertTrue(result)
        coVerify { collection.updateOne(any<Bson>(), any<Bson>(), any()) }
        coVerify { tokenService.removeToken("user", "t1") }
        coVerify { tokenService.removeToken("user", "t2") }
    }

    @Test
    fun `deleteAccount removes user and tokens`() = runBlocking {
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        val tokenService = mockk<FcmTokenService>(relaxed = true)
        val passwordHash = BCrypt.hashpw("pass", BCrypt.gensalt())
        val user = User(username = "user", passwordHash = passwordHash, fcmTokens = listOf(FcmToken("t1", "ts")))
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(listOf(user)))
        coEvery { collection.deleteOne(any<Bson>(), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience", 3600_000, 3600_000, tokenService)

        val result = service.deleteAccount("user", "pass")

        assertTrue(result)
        coVerify { collection.deleteOne(any<Bson>(), any()) }
        coVerify { tokenService.removeToken("user", "t1") }
    }
}
