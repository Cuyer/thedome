package pl.cuyer.thedome.services

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.conversions.Bson
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.AggregateFlow
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.reactivestreams.client.AggregatePublisher
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import com.mongodb.client.model.Collation
import com.mongodb.client.cursor.TimeoutMode
import com.mongodb.ExplainVerbosity
import org.bson.Document
import org.bson.BsonValue
import pl.cuyer.thedome.domain.battlemetrics.*
import pl.cuyer.thedome.domain.rust.Wipe
import pl.cuyer.thedome.domain.rust.RustSettings
import pl.cuyer.thedome.domain.server.*
import pl.cuyer.thedome.util.SimpleFindPublisher

class FiltersServiceTest {
    @Test
    fun `getOptions aggregates data`() = runBlocking {
        val collection = mockk<MongoCollection<BattlemetricsServerContent>>()

        val flagsDocs = listOf(
            BsonDocument("_id", BsonString("GB")),
            BsonDocument("_id", BsonString("US")),
            BsonDocument("_id", BsonString("XX"))
        )
        val mapsDocs = listOf(
            BsonDocument("_id", BsonString("Procedural Map")),
            BsonDocument("_id", BsonString("Barren")),
            BsonDocument("_id", BsonString("Craggy Island"))
        )
        val regionsDocs = listOf(
            BsonDocument("_id", BsonString("Europe/London")),
            BsonDocument("_id", BsonString("Asia/Tokyo")),
            BsonDocument("_id", BsonString("Moon/Base"))
        )
        val difficultyDocs = listOf(
            BsonDocument("_id", BsonString("vanilla")),
            BsonDocument("_id", BsonString("hardcore")),
            BsonDocument("_id", BsonString("unknown"))
        )
        every { collection.aggregate<BsonDocument>(any<List<Bson>>()) } returnsMany listOf(
            AggregateFlow(SimpleAggregatePublisher(flagsDocs)),
            AggregateFlow(SimpleAggregatePublisher(mapsDocs)),
            AggregateFlow(SimpleAggregatePublisher(regionsDocs)),
            AggregateFlow(SimpleAggregatePublisher(difficultyDocs))
        )

        val pubRanking = FindFlow(
            SimpleFindPublisher(
                listOf(BattlemetricsServerContent(attributes = Attributes(id = "r1", rank = 5), id = "1"))
            )
        )
        val pubPlayers = FindFlow(
            SimpleFindPublisher(
                listOf(BattlemetricsServerContent(attributes = Attributes(id = "p1", players = 20), id = "2"))
            )
        )
        val pubGroup = FindFlow(
            SimpleFindPublisher(
                listOf(
                    BattlemetricsServerContent(
                        attributes = Attributes(
                            id = "g1",
                            details = Details(rustSettings = RustSettings(groupLimit = 8))
                        ),
                        id = "3"
                    )
                )
            )
        )
        val pubWipes = FindFlow(
            SimpleFindPublisher(
                listOf(
                    BattlemetricsServerContent(
                        attributes = Attributes(
                            id = "w1",
                            details = Details(rustSettings = RustSettings(wipes = listOf(Wipe(weeks = listOf(1)))))
                        ),
                        id = "4"
                    ),
                    BattlemetricsServerContent(
                        attributes = Attributes(
                            id = "w2",
                            details = Details(rustSettings = RustSettings(wipes = listOf(Wipe(weeks = listOf(1,1,1,1,1)))))
                        ),
                        id = "5"
                    ),
                    BattlemetricsServerContent(
                        attributes = Attributes(
                            id = "w3",
                            details = Details(rustSettings = RustSettings(wipes = List(4) { Wipe(weeks = listOf(1,0,1,0)) }))
                        ),
                        id = "6"
                    )
                )
            )
        )
        every { collection.find() } returnsMany listOf(pubRanking, pubPlayers, pubGroup, pubWipes)

        val service = FiltersService(collection)
        val options = service.getOptions()

        assertEquals(listOf(Flag.GB, Flag.US), options.flags)
        assertEquals(5, options.maxRanking)
        assertEquals(20, options.maxPlayerCount)
        assertEquals(8, options.maxGroupLimit)
        assertEquals(listOf(Maps.BARREN, Maps.CUSTOM, Maps.PROCEDURAL), options.maps)
        assertEquals(listOf(Region.ASIA, Region.EUROPE), options.regions)
        assertEquals(listOf(Difficulty.HARDCORE, Difficulty.VANILLA), options.difficulty)
        assertEquals(
            listOf(WipeSchedule.BIWEEKLY, WipeSchedule.MONTHLY, WipeSchedule.WEEKLY),
            options.wipeSchedules
        )
    }
}

private class SimplePublisher<T>(private val items: List<T> = emptyList()) : Publisher<T> {
    override fun subscribe(s: Subscriber<in T>) {
        s.onSubscribe(object : Subscription {
            override fun request(n: Long) {
                for (item in items) {
                    s.onNext(item)
                }
                s.onComplete()
            }
            override fun cancel() {}
        })
    }
}

private class SimpleAggregatePublisher<T>(private val items: List<T>) : AggregatePublisher<T>, Publisher<T> by SimplePublisher(items) {
    override fun allowDiskUse(allowDiskUse: Boolean?) = this
    override fun maxTime(maxTime: Long, timeUnit: java.util.concurrent.TimeUnit) = this
    override fun maxAwaitTime(maxAwaitTime: Long, timeUnit: java.util.concurrent.TimeUnit) = this
    override fun bypassDocumentValidation(bypassDocumentValidation: Boolean?) = this
    override fun toCollection(): Publisher<Void> = SimplePublisher()
    override fun collation(collation: Collation?) = this
    override fun comment(comment: String?) = this
    override fun comment(comment: BsonValue?) = this
    override fun hint(hint: org.bson.conversions.Bson?) = this
    override fun hintString(hint: String?) = this
    override fun let(variables: org.bson.conversions.Bson?) = this
    override fun batchSize(batchSize: Int) = this
    override fun timeoutMode(timeoutMode: TimeoutMode) = this
    override fun first(): Publisher<T> = SimplePublisher(items.take(1))
    override fun explain(): Publisher<Document> = SimplePublisher()
    override fun explain(verbosity: ExplainVerbosity): Publisher<Document> = SimplePublisher()
    override fun <E> explain(explainResultClass: Class<E>): Publisher<E> = SimplePublisher()
    override fun <E> explain(explainResultClass: Class<E>, verbosity: ExplainVerbosity): Publisher<E> = SimplePublisher()
}
