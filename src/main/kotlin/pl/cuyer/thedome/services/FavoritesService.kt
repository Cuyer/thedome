package pl.cuyer.thedome.services

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.push
import org.litote.kmongo.pull
import pl.cuyer.thedome.domain.auth.User
import pl.cuyer.thedome.domain.battlemetrics.BattlemetricsServerContent
import pl.cuyer.thedome.domain.battlemetrics.toServerInfo
import pl.cuyer.thedome.domain.server.ServersResponse

class FavoritesService(
    private val users: CoroutineCollection<User>,
    private val servers: CoroutineCollection<BattlemetricsServerContent>,
    private val limit: Int
) {
    suspend fun getFavoriteIds(username: String): List<String> {
        val user = users.findOne(User::username eq username)
        return user?.favorites ?: emptyList()
    }
    suspend fun addFavorite(username: String, serverId: String): Boolean {
        val user = users.findOne(User::username eq username) ?: return false
        if (user.favorites.contains(serverId)) return true
        if (!user.subscriber && user.favorites.size >= limit) return false
        users.updateOne(User::username eq username, push(User::favorites, serverId))
        return true
    }

    suspend fun removeFavorite(username: String, serverId: String): Boolean {
        val user = users.findOne(User::username eq username) ?: return false
        if (!user.favorites.contains(serverId)) return false
        users.updateOne(User::username eq username, pull(User::favorites, serverId))
        return true
    }

    suspend fun getFavorites(username: String, page: Int, size: Int): ServersResponse {
        val user = users.findOne(User::username eq username)
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
