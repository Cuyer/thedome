package pl.cuyer.thedome.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.auth.TokenPair
import pl.cuyer.thedome.domain.auth.AccessToken
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

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(username: String, password: String): TokenPair? {
        val existing = collection.findOne(User::username eq username)
        if (existing != null) return null
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        val user = User(username = username, passwordHash = hash, refreshToken = hashedRefresh)
        collection.insertOne(user)
        return TokenPair(generateAccessToken(username), refresh)
    }

    suspend fun registerAnonymous(): AccessToken {
        val username = "anon-${UUID.randomUUID()}"
        val user = User(username = username, passwordHash = "", refreshToken = null)
        collection.insertOne(user)
        return AccessToken(generateAccessToken(username))
    }

    suspend fun upgradeAnonymous(currentUsername: String, newUsername: String, password: String): TokenPair? {
        val anon = collection.findOne(User::username eq currentUsername) ?: return null
        if (!currentUsername.startsWith("anon-") || anon.passwordHash.isNotEmpty()) return null
        if (collection.findOne(User::username eq newUsername) != null) return null
        val hash = BCrypt.hashpw(password, BCrypt.gensalt())
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        collection.updateOne(User::username eq currentUsername, setValue(User::username, newUsername))
        collection.updateOne(User::username eq newUsername, setValue(User::passwordHash, hash))
        collection.updateOne(User::username eq newUsername, setValue(User::refreshToken, hashedRefresh))
        return TokenPair(generateAccessToken(newUsername), refresh)
    }
    
    suspend fun login(username: String, password: String): TokenPair? {
        val user = collection.findOne(User::username eq username) ?: return null
        if (!BCrypt.checkpw(password, user.passwordHash)) return null
        val refresh = generateRefreshToken()
        val hashedRefresh = hashToken(refresh)
        collection.updateOne(User::username eq username, setValue(User::refreshToken, hashedRefresh))
        return TokenPair(generateAccessToken(username), refresh)
    }

    suspend fun refresh(refreshToken: String): TokenPair? {
        val hashed = hashToken(refreshToken)
        val user = collection.findOne(User::refreshToken eq hashed) ?: return null
        val newRefresh = generateRefreshToken()
        val hashedNew = hashToken(newRefresh)
        collection.updateOne(User::username eq user.username, setValue(User::refreshToken, hashedNew))
        return TokenPair(generateAccessToken(user.username), newRefresh)
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
