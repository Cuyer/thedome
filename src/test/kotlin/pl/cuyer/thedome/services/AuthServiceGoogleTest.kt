package pl.cuyer.thedome.services

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertTrue
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.client.model.InsertOneOptions
import org.bson.conversions.Bson
import pl.cuyer.thedome.util.SimpleFindPublisher
import pl.cuyer.thedome.domain.auth.*

class AuthServiceGoogleTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `loginWithGoogle creates user`() = runBlocking {
        val info = GoogleTokenInfo(audience = "client", subject = "sub1", email = "e@example.com")
        val engine = MockEngine { request ->
            respond(
                content = ByteReadChannel(json.encodeToString(info)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        val collection = mockk<MongoCollection<User>>(relaxed = true)
        every { collection.find(any<Bson>()) } returns FindFlow(SimpleFindPublisher(emptyList()))
        coEvery { collection.insertOne(any(), any<InsertOneOptions>()) } returns mockk()
        coEvery { collection.updateOne(any<Bson>(), any<Bson>(), any()) } returns mockk()
        val tokenService = mockk<FcmTokenService>(relaxed = true)
        val service = AuthService(
            collection,
            "secret",
            "issuer",
            "audience",
            3600_000,
            3600_000,
            tokenService,
            "client",
            client
        )

        val result = service.loginWithGoogle("token")

        assertTrue(result?.username == "google-sub1")
        assertTrue(result?.provider == AuthProvider.GOOGLE)
        coVerify { collection.insertOne(match { it.googleId == "sub1" }, any<InsertOneOptions>()) }
    }
}

