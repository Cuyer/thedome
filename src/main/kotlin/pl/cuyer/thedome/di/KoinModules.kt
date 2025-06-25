package pl.cuyer.thedome.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.kotlinx.KotlinSerializerCodecProvider
import com.mongodb.client.model.IndexOptions
import org.bson.Document
import org.koin.dsl.module
import org.koin.core.qualifier.named
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.services.ServerFetchService
import pl.cuyer.thedome.services.ServerCleanupService
import pl.cuyer.thedome.services.ServersService
import pl.cuyer.thedome.services.FiltersService
import pl.cuyer.thedome.services.AuthService
import pl.cuyer.thedome.services.FavoritesService
import pl.cuyer.thedome.services.SubscriptionsService
import pl.cuyer.thedome.services.FcmService
import pl.cuyer.thedome.AppConfig

fun appModule(config: AppConfig) = module {
    single<Json> { Json { ignoreUnknownKeys = true } }

    single<HttpClient> {
        HttpClient(CIO) {
            install(ClientContentNegotiation) { json(get()) }
        }
    }

    single<MongoClient> {
        val registry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(KotlinSerializerCodecProvider())
        )
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(config.mongoUri))
            .codecRegistry(registry)
            .build()
        MongoClient.Factory.create(settings)
    }

    single<MongoDatabase> { get<MongoClient>().getDatabase("thedome") }

    single<MongoCollection<BattlemetricsServerContent>>(named("servers")) {
        val collection = get<MongoDatabase>().getCollection<BattlemetricsServerContent>("servers")
        runBlocking {
            collection.createIndex(Document.parse("{ 'attributes.rank': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.country': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.players': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.details.map': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.details.rust_settings.timeZone': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.details.rust_gamemode': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.details.rust_type': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.details.official': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.details.rust_settings.groupLimit': 1 }"))
            collection.createIndex(Document.parse("{ 'attributes.details.rust_last_wipe': 1 }"))
        }
        collection
    }

    single<MongoCollection<User>>(named("users")) {
        val collection = get<MongoDatabase>().getCollection<User>("users")
        runBlocking {
            collection.createIndex(
                com.mongodb.kotlin.client.model.Indexes.ascending(User::username),
                IndexOptions().unique(true)
            )
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
    single { ServerCleanupService(get(named("servers"))) }
    single { ServersService(get(named("servers"))) }
    single { FiltersService(get(named("servers"))) }
    single {
        AuthService(
            get(named("users")),
            config.jwtSecret,
            config.jwtIssuer,
            config.jwtAudience,
            config.tokenValidity.toLong() * 1000L,
            config.anonTokenValidity.toLong() * 1000L
        )
    }
    single { FavoritesService(get(named("users")), get(named("servers")), config.favoritesLimit) }
    single { SubscriptionsService(get(named("users"))) }
    single {
        FcmService(
            get(),
            get(named("servers")),
            get(),
            config.notifyBeforeWipe,
            config.notifyBeforeMapWipe
        )
    }
}

