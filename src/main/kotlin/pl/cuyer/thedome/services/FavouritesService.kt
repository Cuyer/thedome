package pl.cuyer.thedome.services

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.model.Filters.eq
import com.mongodb.kotlin.client.model.Updates.push
import com.mongodb.kotlin.client.model.Updates.pull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.toServerInfo
import pl.cuyer.thedome.domain.server.ServersResponse

class FavouritesService(
    private val users: MongoCollection<User>,
    private val servers: MongoCollection<BattlemetricsServerContent>,
    private val limit: Int
) {
    suspend fun getFavouriteIds(username: String): List<String> {
        val user = users.find(eq(User::username, username)).firstOrNull()
        return user?.favourites ?: emptyList()
    }
    suspend fun addFavourite(username: String, serverId: String): Boolean {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return false
        if (user.favourites.contains(serverId)) return true
        if (!user.subscriber && user.favourites.size >= limit) return false
        users.updateOne(eq(User::username, username), push(User::favourites, serverId))
        return true
    }

    suspend fun removeFavourite(username: String, serverId: String): Boolean {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return false
        if (!user.favourites.contains(serverId)) return false
        users.updateOne(eq(User::username, username), pull(User::favourites, serverId))
        return true
    }

    suspend fun getFavourites(username: String, page: Int, size: Int): ServersResponse {
        val user = users.find(eq(User::username, username)).firstOrNull()
        val favourites = user?.favourites ?: emptyList()
        val skip = (page - 1) * size
        val query = if (favourites.isEmpty()) {
            emptyList<BattlemetricsServerContent>()
        } else {
            servers.find(Filters.`in`("id", favourites))
                .sort(Sorts.ascending("attributes.rank"))
                .skip(skip)
                .limit(size)
                .toList()
        }
        val serverInfos = query.map { it.toServerInfo() }
        val totalItems = favourites.size.toLong()
        val totalPages = if (size == 0) 0 else ((totalItems + size - 1) / size).toInt()
        return ServersResponse(page, size, totalPages, totalItems, serverInfos)
    }
}
