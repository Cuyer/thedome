package pl.cuyer.thedome.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/favorites")
data class Favorites(
    val page: Int? = null,
    val size: Int? = null
)
