package online.omniroute.chat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChatStore(context: Context) {
    private val file = File(context.filesDir, "omniroute-chats-v1.json")

    data class Snapshot(
        val conversations: List<Conversation> = emptyList(),
        val settings: ProviderSettings = ProviderSettings(),
        val defaultRoute: RouteId = RouteId.AUTO,
    )

    @Synchronized
    fun load(): Snapshot {
        if (!file.exists()) return Snapshot()
        return try {
            val root = JSONObject(file.readText())
            val settingsObj = root.optJSONObject("settings") ?: JSONObject()
            val settings = ProviderSettings(
                mode = settingsObj.optString("mode", ProviderSettings.MODE_FREE),
                baseUrl = settingsObj.optString("baseUrl", ProviderSettings.DEFAULT_OMNI_URL),
                apiKey = settingsObj.optString("apiKey", ""),
                model = settingsObj.optString("model", "auto"),
            )
            val convos = mutableListOf<Conversation>()
            val arr = root.optJSONArray("conversations") ?: JSONArray()
            for (i in 0 until arr.length()) {
                convos += parseConversation(arr.getJSONObject(i))
            }
            Snapshot(
                conversations = convos.sortedByDescending { it.updatedAt },
                settings = settings,
                defaultRoute = RouteId.fromId(root.optString("defaultRoute", "auto")),
            )
        } catch (_: Exception) {
            Snapshot()
        }
    }

    @Synchronized
    fun save(snapshot: Snapshot) {
        val convos = JSONArray()
        snapshot.conversations.forEach { convos.put(toJson(it)) }
        val settings = JSONObject()
            .put("mode", snapshot.settings.mode)
            .put("baseUrl", snapshot.settings.baseUrl)
            .put("apiKey", snapshot.settings.apiKey)
            .put("model", snapshot.settings.model)
        val root = JSONObject()
            .put("conversations", convos)
            .put("settings", settings)
            .put("defaultRoute", snapshot.defaultRoute.id)
        file.writeText(root.toString())
    }

    private fun parseConversation(obj: JSONObject): Conversation {
        val messages = mutableListOf<ChatMessage>()
        val arr = obj.optJSONArray("messages") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val m = arr.getJSONObject(i)
            messages += ChatMessage(
                id = m.optString("id"),
                role = m.optString("role"),
                content = m.optString("content"),
                createdAt = m.optLong("createdAt"),
                error = m.optString("error").ifBlank { null },
            )
        }
        return Conversation(
            id = obj.optString("id"),
            title = obj.optString("title", "New chat"),
            route = RouteId.fromId(obj.optString("route")),
            messages = messages,
            createdAt = obj.optLong("createdAt"),
            updatedAt = obj.optLong("updatedAt"),
        )
    }

    private fun toJson(c: Conversation): JSONObject {
        val messages = JSONArray()
        c.messages.forEach { m ->
            messages.put(
                JSONObject()
                    .put("id", m.id)
                    .put("role", m.role)
                    .put("content", m.content)
                    .put("createdAt", m.createdAt)
                    .put("error", m.error ?: ""),
            )
        }
        return JSONObject()
            .put("id", c.id)
            .put("title", c.title)
            .put("route", c.route.id)
            .put("createdAt", c.createdAt)
            .put("updatedAt", c.updatedAt)
            .put("messages", messages)
    }
}
