package pl.cuyer.thedome.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/servers")
data class Servers(val page: Int? = null, val size: Int? = null)
