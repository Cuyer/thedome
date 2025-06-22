package pl.cuyer.thedome.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.auth.TokenPair
import pl.cuyer.thedome.domain.auth.AccessToken
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.UUID
import java.security.MessageDigest
import org.mindrot.jbcrypt.BCrypt

class AuthService(
    private val collection: CoroutineCollection<User>,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String
) {
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(username: String, email: String, password: String): TokenPair? {
        logger.info("Registering user: $username")
        val existing = collection.findOne(User::username eq username) ?: collection.findOne(User::email eq email)
        if (existing != null) return null
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        val user = User(username = username, email = email, passwordHash = hash, refreshToken = hashedRefresh)
        collection.insertOne(user)
        logger.info("User $username registered")
        return TokenPair(generateAccessToken(username), refresh, username, email)
    }

    suspend fun registerAnonymous(): AccessToken {
        val username = "anon-${UUID.randomUUID()}"
        logger.info("Registering anonymous user $username")
        val user = User(username = username, passwordHash = "", refreshToken = null)
        collection.insertOne(user)
        logger.info("Anonymous user $username registered")
        return AccessToken(generateAccessToken(username), username)
    }

    suspend fun upgradeAnonymous(currentUsername: String, newUsername: String, password: String): TokenPair? {
        logger.info("Upgrading anonymous user $currentUsername to $newUsername")
        val anon = collection.findOne(User::username eq currentUsername) ?: return null
        if (!currentUsername.startsWith("anon-") || anon.passwordHash.isNotEmpty()) return null
        if (collection.findOne(User::username eq newUsername) != null) return null
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        collection.updateOne(User::username eq currentUsername, setValue(User::username, newUsername))
        collection.updateOne(User::username eq newUsername, setValue(User::passwordHash, hash))
        collection.updateOne(User::username eq newUsername, setValue(User::refreshToken, hashedRefresh))
        logger.info("Anonymous user $currentUsername upgraded to $newUsername")
        val updated = collection.findOne(User::username eq newUsername)
        return TokenPair(generateAccessToken(newUsername), refresh, newUsername, updated?.email)
    }

    suspend fun login(username: String, password: String): TokenPair? {
        logger.info("User $username attempting login")
        val user = collection.findOne(User::username eq username) ?: collection.findOne(User::email eq username) ?: return null
        if (!BCrypt.checkpw(password, user.passwordHash)) return null
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        collection.updateOne(User::username eq user.username, setValue(User::refreshToken, hashedRefresh))
        logger.info("User ${'$'}{user.username} logged in")
        return TokenPair(generateAccessToken(user.username), refresh, user.username, user.email)
    }

    suspend fun refresh(refreshToken: String): TokenPair? {
        logger.info("Refreshing token")
        val hashed = hashToken(refreshToken)
        val user = collection.findOne(User::refreshToken eq hashed) ?: return null
        val newRefresh = generateRefreshToken()
        val hashedNew = hashToken(newRefresh)
        collection.updateOne(User::username eq user.username, setValue(User::refreshToken, hashedNew))
        logger.info("Issued new refresh token for ${'$'}{user.username}")
        return TokenPair(generateAccessToken(user.username), newRefresh, user.username, user.email)
    }

    private fun generateAccessToken(username: String): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600_000))
            .sign(algorithm)
    }

    private fun generateRefreshToken(): String = UUID.randomUUID().toString()
}
