package pl.cuyer.thedome.util

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import com.mongodb.reactivestreams.client.FindPublisher
import com.mongodb.client.model.Collation
import com.mongodb.client.cursor.TimeoutMode
import com.mongodb.CursorType
import com.mongodb.ExplainVerbosity
import org.bson.Document
import org.bson.BsonValue
import org.bson.conversions.Bson

class SimplePublisher<T>(private val items: List<T> = emptyList()) : Publisher<T> {
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

class SimpleFindPublisher<T>(private val items: List<T>) : FindPublisher<T>, Publisher<T> by SimplePublisher(items) {
    override fun first(): Publisher<T> = SimplePublisher(items.take(1))
    override fun filter(filter: Bson?) = this
    override fun limit(limit: Int) = this
    override fun skip(skip: Int) = this
    override fun maxTime(maxTime: Long, timeUnit: java.util.concurrent.TimeUnit) = this
    override fun maxAwaitTime(maxAwaitTime: Long, timeUnit: java.util.concurrent.TimeUnit) = this
    override fun projection(projection: Bson?) = this
    override fun sort(sort: Bson?) = this
    override fun noCursorTimeout(noCursorTimeout: Boolean) = this
    override fun partial(partial: Boolean) = this
    override fun cursorType(cursorType: CursorType) = this
    override fun collation(collation: Collation?) = this
    override fun comment(comment: String?) = this
    override fun comment(comment: BsonValue?) = this
    override fun hint(hint: Bson?) = this
    override fun hintString(hint: String?) = this
    override fun let(variables: Bson?) = this
    override fun max(max: Bson?) = this
    override fun min(min: Bson?) = this
    override fun returnKey(returnKey: Boolean) = this
    override fun showRecordId(showRecordId: Boolean) = this
    override fun batchSize(batchSize: Int) = this
    override fun allowDiskUse(allowDiskUse: Boolean?) = this
    override fun timeoutMode(timeoutMode: TimeoutMode) = this
    override fun explain(): Publisher<Document> = SimplePublisher()
    override fun explain(verbosity: ExplainVerbosity): Publisher<Document> = SimplePublisher()
    override fun <E> explain(explainResultClass: Class<E>): Publisher<E> = SimplePublisher()
    override fun <E> explain(explainResultClass: Class<E>, verbosity: ExplainVerbosity): Publisher<E> = SimplePublisher()
}
