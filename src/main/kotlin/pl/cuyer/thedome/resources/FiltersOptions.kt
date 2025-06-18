package pl.cuyer.thedome.resources

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/filters/options")
class FiltersOptions
