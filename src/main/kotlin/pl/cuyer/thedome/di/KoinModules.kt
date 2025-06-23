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
import com.mongodb.client.model.IndexOptions
import org.bson.Document
import org.koin.dsl.module
import org.koin.core.qualifier.named
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

    single<CoroutineCollection<BattlemetricsServerContent>>(named("servers")) {
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

    single<CoroutineCollection<User>>(named("users")) {
        val collection = get<CoroutineDatabase>().getCollection<User>("users")
        runBlocking {
            collection.ensureUniqueIndex(User::username)
            val partial = Document("email", Document("\$type", "string"))
            val options = IndexOptions()
                .name("email_1")
                .unique(true)
                .partialFilterExpression(partial)
            collection.createIndex(Document("email", 1), options)
        }
        collection
    }

    single { ServerFetchService(get(), get(named("servers")), config.apiKey) }
    single { ServersService(get(named("servers"))) }
    single { FiltersService(get(named("servers"))) }
    single { AuthService(get(named("users")), config.jwtSecret, config.jwtIssuer, config.jwtAudience) }
}
