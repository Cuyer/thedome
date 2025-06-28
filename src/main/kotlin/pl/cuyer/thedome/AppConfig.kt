package pl.cuyer.thedome

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val allowedOrigins: String?,
    val jwtAudience: String,
    val jwtIssuer: String,
    val jwtRealm: String,
    val jwtSecret: String,
    val fetchCron: String,
    val cleanupCron: String,
    val resubscribeCron: String,
    val mongoUri: String,
    val apiKey: String,
    val anonRateLimit: Int,
    val anonRefillPeriod: Int,
    val favouritesLimit: Int,
    val subscriptionsLimit: Int,
    val tokenValidity: Int,
    val anonTokenValidity: Int,
    val notificationCron: String,
    val notifyBeforeWipe: List<Int>,
    val notifyBeforeMapWipe: List<Int>
) {
    companion object {
        fun load(config: ApplicationConfig): AppConfig {
            val section = config.config("ktor.config")
            val allowedOrigins = section.propertyOrNull("allowedOrigins")?.getString()
            val jwtSection = section.config("jwt")
            val jwtSecret = jwtSection.property("secret").getString()
            val jwtAudience = jwtSection.propertyOrNull("audience")?.getString() ?: "thedomeAudience"
            val jwtIssuer = jwtSection.propertyOrNull("issuer")?.getString() ?: "thedomeIssuer"
            val jwtRealm = jwtSection.propertyOrNull("realm")?.getString() ?: "thedomeRealm"
            val fetchCron = section.propertyOrNull("fetchCron")?.getString() ?: "0 */10 * * *"
            val cleanupCron = section.propertyOrNull("cleanupCron")?.getString() ?: "0 0 * * *"
            val resubscribeCron = section.propertyOrNull("resubscribeCron")?.getString() ?: "0 0 1 * *"
            val mongoUri = section.propertyOrNull("mongoUri")?.getString() ?: "mongodb://localhost:27017"
            val apiKey = section.propertyOrNull("apiKey")?.getString() ?: ""
            val anonRateLimit = section.propertyOrNull("anonRateLimit")?.getString()?.toIntOrNull() ?: 60
            val anonRefillPeriod = section.propertyOrNull("anonRefillPeriod")?.getString()?.toIntOrNull() ?: 60
            val favouritesLimit = section.propertyOrNull("favouritesLimit")?.getString()?.toIntOrNull() ?: 10
            val subscriptionsLimit = section.propertyOrNull("subscriptionsLimit")?.getString()?.toIntOrNull() ?: 10
            val tokenValidity = section.propertyOrNull("tokenValidity")?.getString()?.toIntOrNull() ?: 3600
            val anonTokenValidity = section.propertyOrNull("anonTokenValidity")?.getString()?.toIntOrNull() ?: 3600
            val notificationCron = section.propertyOrNull("notificationCron")?.getString() ?: "0 * * * *"
            val notifyBeforeWipe = section.propertyOrNull("notifyBeforeWipe")
                ?.getString()
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList()
            val notifyBeforeMapWipe = section.propertyOrNull("notifyBeforeMapWipe")
                ?.getString()
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList()
            return AppConfig(
                allowedOrigins,
                jwtAudience,
                jwtIssuer,
                jwtRealm,
                jwtSecret,
                fetchCron,
                cleanupCron,
                resubscribeCron,
                mongoUri,
                apiKey,
                anonRateLimit,
                anonRefillPeriod,
                favouritesLimit,
                subscriptionsLimit,
                tokenValidity,
                anonTokenValidity,
                notificationCron,
                notifyBeforeWipe,
                notifyBeforeMapWipe
            )
        }
    }
}
