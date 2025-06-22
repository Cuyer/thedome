package pl.cuyer.thedome.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.coroutine.coroutine
import org.koin.dsl.module
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.services.ServerFetchService
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.services.FiltersService
import pl.cuyer.thedome.services.AuthService
import pl.cuyer.thedome.AppConfig

fun appModule(config: AppConfig) = module {
    single<Json> { Json { ignoreUnknownKeys = true } }

    single<HttpClient> {
        HttpClient(CIO) {
            install(ClientContentNegotiation) { json(get()) }
        }
    }

    single<CoroutineClient> {
        KMongo.createClient(config.mongoUri).coroutine
    }

    single<CoroutineDatabase> { get<CoroutineClient>().getDatabase("thedome") }

    single<CoroutineCollection<BattlemetricsServerContent>> {
        val collection = get<CoroutineDatabase>().getCollection<BattlemetricsServerContent>("servers")
        runBlocking {
            collection.createIndex("{ 'attributes.rank': 1 }")
            collection.createIndex("{ 'attributes.country': 1 }")
            collection.createIndex("{ 'attributes.players': 1 }")
            collection.createIndex("{ 'attributes.details.map': 1 }")
            collection.createIndex("{ 'attributes.details.rust_settings.timeZone': 1 }")
            collection.createIndex("{ 'attributes.details.rust_gamemode': 1 }")
            collection.createIndex("{ 'attributes.details.rust_type': 1 }")
            collection.createIndex("{ 'attributes.details.official': 1 }")
            collection.createIndex("{ 'attributes.details.rust_settings.groupLimit': 1 }")
            collection.createIndex("{ 'attributes.details.rust_last_wipe': 1 }")
        }
        collection
    }

    single<CoroutineCollection<User>> {
        val collection = get<CoroutineDatabase>().getCollection<User>("users")
        runBlocking {
            collection.ensureUniqueIndex(User::username)
        }
        collection
    }

    single { ServerFetchService(get(), get(), config.apiKey) }
    single { ServersService(get()) }
    single { FiltersService(get()) }
    single { AuthService(get(), config.jwtSecret, config.jwtIssuer, config.jwtAudience) }
}
