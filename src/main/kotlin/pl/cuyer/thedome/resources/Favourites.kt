package pl.cuyer.thedome.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/favourites")
data class Favourites(
    val page: Int? = null,
    val size: Int? = null
)
