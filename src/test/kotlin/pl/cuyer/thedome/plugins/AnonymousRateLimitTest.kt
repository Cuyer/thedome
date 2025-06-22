package pl.cuyer.thedome.plugins

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import java.util.concurrent.ConcurrentHashMap

class AnonymousRateLimitTest {
    @Test
    fun `cleanup removes expired entries`() = runBlocking {
        val requests = ConcurrentHashMap<String, RateInfo>()
        val job = launchCleanupJob(this, requests, ttl = 50, interval = 10)
        requests["anon-1"] = RateInfo(1, System.currentTimeMillis() - 100)
        kotlinx.coroutines.delay(60)
        job.cancel()
        job.join()
        assertTrue(requests.isEmpty())
    }
}
