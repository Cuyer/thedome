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

class FavoritesService(
    private val users: MongoCollection<User>,
    private val servers: MongoCollection<BattlemetricsServerContent>,
    private val limit: Int
) {
    suspend fun getFavoriteIds(username: String): List<String> {
        val user = users.find(eq(User::username, username)).firstOrNull()
        return user?.favorites ?: emptyList()
    }
    suspend fun addFavorite(username: String, serverId: String): Boolean {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return false
        if (user.favorites.contains(serverId)) return true
        if (!user.subscriber && user.favorites.size >= limit) return false
        users.updateOne(eq(User::username, username), push(User::favorites, serverId))
        return true
    }

    suspend fun removeFavorite(username: String, serverId: String): Boolean {
        val user = users.find(eq(User::username, username)).firstOrNull() ?: return false
        if (!user.favorites.contains(serverId)) return false
        users.updateOne(eq(User::username, username), pull(User::favorites, serverId))
        return true
    }

    suspend fun getFavorites(username: String, page: Int, size: Int): ServersResponse {
        val user = users.find(eq(User::username, username)).firstOrNull()
        val favorites = user?.favorites ?: emptyList()
        val skip = (page - 1) * size
        val query = if (favorites.isEmpty()) {
            emptyList<BattlemetricsServerContent>()
        } else {
            servers.find(Filters.`in`("id", favorites))
                .sort(Sorts.ascending("attributes.rank"))
                .skip(skip)
                .limit(size)
                .toList()
        }
        val serverInfos = query.map { it.toServerInfo() }
        val totalItems = favorites.size.toLong()
        val totalPages = if (size == 0) 0 else ((totalItems + size - 1) / size).toInt()
        return ServersResponse(page, size, totalPages, totalItems, serverInfos)
    }
}
