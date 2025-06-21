package pl.cuyer.thedome.services

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import org.litote.kmongo.coroutine.CoroutineCollection
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
}
