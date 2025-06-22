package pl.cuyer.thedome.exceptions

open class ServersException(message: String) : RuntimeException(message)
class ServersQueryException : ServersException("Unable to query servers")

open class FiltersException(message: String) : RuntimeException(message)
class FiltersOptionsException : FiltersException("Unable to fetch filter options")
