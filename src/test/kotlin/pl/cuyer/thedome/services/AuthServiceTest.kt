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

class AuthServiceTest {
    @Test
    fun `registerAnonymous stores user`() = runBlocking {
        val collection = mockk<CoroutineCollection<User>>()
        coEvery { collection.insertOne(any(), any<InsertOneOptions>()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience")

        val result = service.registerAnonymous()

        assertTrue(result.accessToken.isNotEmpty())
        coVerify { collection.insertOne(match { it.username.startsWith("anon-") && it.refreshToken == null }, any<InsertOneOptions>()) }
    }

    @Test
    fun `upgradeAnonymous converts user`() = runBlocking {
        val collection = mockk<CoroutineCollection<User>>()
        val anon = User(username = "anon-123", passwordHash = "", refreshToken = null)
        coEvery { collection.findOne(any<Bson>()) } returnsMany listOf(anon, null)
        coEvery { collection.updateOne(any<Bson>(), any<Bson>(), any()) } returns mockk()
        val service = AuthService(collection, "secret", "issuer", "audience")

        val result = service.upgradeAnonymous("anon-123", "newuser", "pass")

        assertTrue(result?.accessToken?.isNotEmpty() == true)
        coVerify(exactly = 3) { collection.updateOne(any<Bson>(), any<Bson>(), any()) }
    }
}
