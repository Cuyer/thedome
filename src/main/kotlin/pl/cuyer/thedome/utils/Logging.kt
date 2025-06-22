package pl.cuyer.thedome.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("pl.cuyer.thedome.Application")

fun logException(call: ApplicationCall, cause: Throwable) {
    val method = call.request.httpMethod.value
    val uri = call.request.uri
    logger.error("Exception during $method $uri: ${'$'}{cause.message}", cause)
}
