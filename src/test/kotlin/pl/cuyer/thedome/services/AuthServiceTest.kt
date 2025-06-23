package pl.cuyer.thedome.services

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.bson.conversions.Bson
import pl.cuyer.thedome.domain.auth.User
import com.mongodb.client.model.InsertOneOptions
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import io.mockk.slot

class AuthServiceTest {

    private fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    @Test
    fun `registerAnonymous stores user`() = runBlocking {
        val collection = mockk<CoroutineCollection<User>>()
        coEvery { collection.insertOne(any(), any<InsertOneOptions>()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience")

        val result = service.registerAnonymous()

        assertTrue(result.accessToken.isNotEmpty())
        assertTrue(result.username.startsWith("anon-"))
        coVerify { collection.insertOne(match { it.username.startsWith("anon-") && it.refreshToken == null && it.email == null }, any<InsertOneOptions>()) }
    }
    @Test
    fun `upgradeAnonymous converts user`() = runBlocking {
        val collection = mockk<CoroutineCollection<User>>()
        val anon = User(username = "anon-123", email = null, passwordHash = "", refreshToken = null, favorites = emptyList())
        val updated = anon.copy(username = "newuser")
        coEvery { collection.findOne(any<Bson>()) } returnsMany listOf(anon, null, updated)
        coEvery { collection.updateOne(any<Bson>(), any<Bson>(), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience")

        val result = service.upgradeAnonymous("anon-123", "newuser", "pass")

        assertTrue(result?.accessToken?.isNotEmpty() == true)
        coVerify(exactly = 3) { collection.updateOne(any<Bson>(), any<Bson>(), any()) }
    }

    @Test
    fun `login hashes refresh token`() = runBlocking {
        val collection = mockk<CoroutineCollection<User>>()
        val passwordHash = BCrypt.hashpw("pass", BCrypt.gensalt())
        val user = User(username = "user", email = "user@example.com", passwordHash = passwordHash, favorites = emptyList())
        val slotUpdate = slot<Bson>()
        coEvery { collection.findOne(any<Bson>()) } returns user
        coEvery { collection.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience")

        val result = service.login("user", "pass")

        assertTrue(result?.refreshToken?.isNotEmpty() == true)
        assertTrue(result?.username == "user")
        assertTrue(result?.email == "user@example.com")
        val expected = hash(result!!.refreshToken)
        assertTrue(slotUpdate.captured.toString().contains(expected))
    }

    @Test
    fun `refresh compares hashed token`() = runBlocking {
        val collection = mockk<CoroutineCollection<User>>()
        val oldToken = "old-token"
        val oldHash = hash(oldToken)
        val user = User(username = "user", email = "user@example.com", passwordHash = "", refreshToken = oldHash, favorites = emptyList())
        val slotFind = slot<Bson>()
        val slotUpdate = slot<Bson>()
        coEvery { collection.findOne(capture(slotFind)) } returns user
        coEvery { collection.updateOne(any<Bson>(), capture(slotUpdate), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience")

        val result = service.refresh(oldToken)

        assertTrue(result?.refreshToken?.isNotEmpty() == true)
        assertTrue(result?.username == "user")
        assertTrue(result?.email == "user@example.com")
        val expectedFind = slotFind.captured.toString()
        assertTrue(expectedFind.contains(oldHash))
        val expectedUpdate = hash(result!!.refreshToken)
        assertTrue(slotUpdate.captured.toString().contains(expectedUpdate))
    }
}
