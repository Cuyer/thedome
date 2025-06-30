package pl.cuyer.thedome.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.model.Filters.eq
import com.mongodb.client.model.Filters.eq as eqStr
import com.mongodb.kotlin.client.model.Updates.set
import kotlinx.coroutines.flow.firstOrNull
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.auth.TokenPair
import pl.cuyer.thedome.domain.auth.AccessToken
import pl.cuyer.thedome.domain.auth.GoogleTokenInfo
import pl.cuyer.thedome.services.FcmTokenService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.UUID
import java.security.MessageDigest
import org.mindrot.jbcrypt.BCrypt
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

class AuthService(
    private val collection: MongoCollection<User>,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String,
    private val tokenValidityMs: Long,
    private val anonTokenValidityMs: Long,
    private val tokenService: FcmTokenService,
    private val googleClientId: String,
    private val client: HttpClient
) {
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(username: String, email: String, password: String): TokenPair? {
        logger.info("Registering user: $username")
        val existing = collection.find(eq(User::username, username)).firstOrNull() ?: collection.find(eq(User::email, email)).firstOrNull()
        if (existing != null) return null
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        val user = User(username = username, email = email, passwordHash = hash, refreshToken = hashedRefresh)
        collection.insertOne(user)
        logger.info("User $username registered")
        return TokenPair(generateAccessToken(user, tokenValidityMs), refresh, username, email)
    }

    suspend fun registerAnonymous(): AccessToken {
        val username = "anon-${UUID.randomUUID()}"
        logger.info("Registering anonymous user $username")
        val expires = Clock.System.now() + anonTokenValidityMs.milliseconds
        val user = User(
            username = username,
            passwordHash = "",
            refreshToken = null,
            testEndsAt = expires.toString()
        )
        collection.insertOne(user)
        logger.info("Anonymous user $username registered")
        return AccessToken(generateAccessToken(user, anonTokenValidityMs), username)
    }

    suspend fun upgradeAnonymous(currentUsername: String, newUsername: String, password: String): TokenPair? {
        logger.info("Upgrading anonymous user $currentUsername to $newUsername")
        val anon = collection.find(eq(User::username, currentUsername)).firstOrNull() ?: return null
        if (!currentUsername.startsWith("anon-") || anon.passwordHash.isNotEmpty()) return null
        if (collection.find(eq(User::username, newUsername)).firstOrNull() != null) return null
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        collection.updateOne(eq(User::username, currentUsername), set(User::username, newUsername))
        collection.updateOne(eq(User::username, newUsername), set(User::passwordHash, hash))
        collection.updateOne(eq(User::username, newUsername), set(User::refreshToken, hashedRefresh))
        logger.info("Anonymous user $currentUsername upgraded to $newUsername")
        val updated = collection.find(eq(User::username, newUsername)).firstOrNull() ?: return null
        return TokenPair(generateAccessToken(updated, tokenValidityMs), refresh, newUsername, updated.email)
    }

    suspend fun login(username: String, password: String): TokenPair? {
        logger.info("User $username attempting login")
        val user = collection.find(eq(User::username, username)).firstOrNull() ?: collection.find(eq(User::email, username)).firstOrNull() ?: return null
        if (!BCrypt.checkpw(password, user.passwordHash)) return null
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        collection.updateOne(eq(User::username, user.username), set(User::refreshToken, hashedRefresh))
        tokenService.resubscribeUserTokens(user.username)
        logger.info("User ${user.username} logged in")
        return TokenPair(generateAccessToken(user, tokenValidityMs), refresh, user.username, user.email)
    }

    suspend fun loginWithGoogle(token: String): TokenPair? {
        logger.info("Verifying Google token")
        val info = client.get("https://oauth2.googleapis.com/tokeninfo") {
            url {
                parameters.append("id_token", token)
            }
        }.body<GoogleTokenInfo>()
        if (info.audience != googleClientId) return null
        val googleId = info.subject
        var user = collection.find(eq(User::googleId, googleId)).firstOrNull()
        if (user == null) {
            val email = info.email
            var base = email?.substringBefore("@") ?: "google-$googleId"
            var name = base
            var i = 1
            while (collection.find(eq(User::username, name)).firstOrNull() != null) {
                name = "$base$i"
                i++
            }
            user = User(username = name, email = email, googleId = googleId, passwordHash = "")
            collection.insertOne(user)
        } else if (user.googleId == null) {
            collection.updateOne(eq(User::username, user.username), set(User::googleId, googleId))
            user = user.copy(googleId = googleId)
        }
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        collection.updateOne(eq(User::username, user.username), set(User::refreshToken, hashedRefresh))
        tokenService.resubscribeUserTokens(user.username)
        logger.info("User ${user.username} logged in via Google")
        return TokenPair(generateAccessToken(user, tokenValidityMs), refresh, user.username, user.email)
    }

    suspend fun refresh(refreshToken: String): TokenPair? {
        logger.info("Refreshing token")
        val hashed = hashToken(refreshToken)
        val user = collection.find(eq(User::refreshToken, hashed)).firstOrNull() ?: return null
        val newRefresh = generateRefreshToken()
        val hashedNew = hashToken(newRefresh)
        collection.updateOne(eq(User::username, user.username), set(User::refreshToken, hashedNew))
        logger.info("Issued new refresh token for ${user.username}")
        return TokenPair(generateAccessToken(user, tokenValidityMs), newRefresh, user.username, user.email)
    }

    suspend fun logout(username: String): Boolean {
        val user = collection.find(eq(User::username, username)).firstOrNull() ?: return false
        collection.updateOne(eq(User::username, username), set(User::refreshToken, null))
        for (t in user.fcmTokens) {
            tokenService.removeToken(username, t.token)
        }
        return true
    }

    suspend fun deleteAccount(username: String, password: String?): Boolean {
        val user = collection.find(eq(User::username, username)).firstOrNull() ?: return false
        if (user.googleId == null) {
            if (password == null || !BCrypt.checkpw(password, user.passwordHash)) return false
        }
        for (t in user.fcmTokens) {
            tokenService.removeToken(username, t.token)
        }
        collection.deleteOne(eq(User::username, username))
        return true
    }

    private fun generateAccessToken(user: User, validity: Long): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("username", user.username)
            .withClaim("email", user.email)
            .withExpiresAt(Date(System.currentTimeMillis() + validity))
            .sign(algorithm)
    }

    private fun generateRefreshToken(): String = UUID.randomUUID().toString()
}
