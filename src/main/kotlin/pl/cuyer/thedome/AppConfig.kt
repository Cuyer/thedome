package pl.cuyer.thedome

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val allowedOrigins: String?,
    val jwtAudience: String,
    val jwtIssuer: String,
    val jwtRealm: String,
    val jwtSecret: String,
    val fetchCron: String,
    val mongoUri: String,
    val apiKey: String,
    val anonRateLimit: Int,
    val anonRefillPeriod: Int
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
            val mongoUri = section.propertyOrNull("mongoUri")?.getString() ?: "mongodb://localhost:27017"
            val apiKey = section.propertyOrNull("apiKey")?.getString() ?: ""
            val anonRateLimit = section.propertyOrNull("anonRateLimit")?.getString()?.toIntOrNull() ?: 60
            val anonRefillPeriod = section.propertyOrNull("anonRefillPeriod")?.getString()?.toIntOrNull() ?: 60
            return AppConfig(
                allowedOrigins,
                jwtAudience,
                jwtIssuer,
                jwtRealm,
                jwtSecret,
                fetchCron,
                mongoUri,
                apiKey,
                anonRateLimit,
                anonRefillPeriod
            )
        }
    }
}
