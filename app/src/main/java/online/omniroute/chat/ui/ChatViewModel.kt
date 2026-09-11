package online.omniroute.chat.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.omniroute.chat.data.ChatMessage
import online.omniroute.chat.data.ChatStore
import online.omniroute.chat.data.ChatTurn
import online.omniroute.chat.data.Conversation
import online.omniroute.chat.data.OpenAiClient
import online.omniroute.chat.data.ProviderSettings
import online.omniroute.chat.data.RouteId
import online.omniroute.chat.data.titleFromPrompt
import java.util.UUID

sealed interface Screen {
    data object List : Screen
    data class Thread(val chatId: String) : Screen
    data object Settings : Screen
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ChatStore(app)
    private val client = OpenAiClient()

    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var settings by mutableStateOf(ProviderSettings())
        private set
    var defaultRoute by mutableStateOf(RouteId.AUTO)
        private set
    var screen by mutableStateOf<Screen>(Screen.List)
    var draft by mutableStateOf("")
    var search by mutableStateOf("")
    var streamText by mutableStateOf("")
        private set
    var streamingChatId by mutableStateOf<String?>(null)
        private set
    var streamingMessageId by mutableStateOf<String?>(null)
        private set
    var toast by mutableStateOf<String?>(null)

    private var job: Job? = null

    init {
        val snap = store.load()
        conversations = snap.conversations
        settings = snap.settings
        defaultRoute = snap.defaultRoute
    }

    val filtered: List<Conversation>
        get() {
            val q = search.trim().lowercase()
            if (q.isEmpty()) return conversations
            return conversations.filter {
                it.title.lowercase().contains(q) ||
                    it.messages.any { m -> m.content.lowercase().contains(q) }
            }
        }

    fun active(chatId: String?): Conversation? =
        conversations.find { it.id == chatId }

    fun openList() {
        screen = Screen.List
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    fun openChat(id: String) {
        screen = Screen.Thread(id)
        draft = ""
    }

    fun newChat(route: RouteId = defaultRoute): String {
        stop()
        val chat = Conversation(route = route)
        conversations = listOf(chat) + conversations
        persist()
        openChat(chat.id)
        return chat.id
    }

    fun deleteChat(id: String) {
        if (streamingChatId == id) stop()
        conversations = conversations.filter { it.id != id }
        persist()
        screen = Screen.List
    }

    fun clearAll() {
        stop()
        conversations = emptyList()
        persist()
        screen = Screen.List
        toast = "All chats cleared"
    }

    fun setRoute(chatId: String, route: RouteId) {
        conversations = conversations.map {
            if (it.id == chatId) it.copy(route = route) else it
        }
        defaultRoute = route
        persist()
    }

    fun saveSettings(next: ProviderSettings) {
        settings = next
        persist()
        toast = "Settings saved"
    }

    fun send(text: String, chatId: String? = null, route: RouteId? = null) {
        val prompt = text.trim()
        if (prompt.isEmpty() || streamingChatId != null) return
        val id = chatId ?: (screen as? Screen.Thread)?.chatId ?: newChat(route ?: defaultRoute)
        if (route != null) setRoute(id, route)
        draft = ""
        val user = ChatMessage(role = "user", content = prompt)
        val assistant = ChatMessage(role = "assistant", content = "")
        conversations = conversations.map { c ->
            if (c.id != id) c
            else c.copy(
                title = if (c.messages.isEmpty()) titleFromPrompt(prompt) else c.title,
                updatedAt = System.currentTimeMillis(),
                messages = c.messages + user + assistant,
            )
        }.sortedByDescending { it.updatedAt }
        persist()
        openChat(id)
        startStream(id, assistant.id)
    }

    fun retryLast(chatId: String) {
        if (streamingChatId != null) return
        val chat = active(chatId) ?: return
        if (chat.messages.none { it.role == "user" }) return
        val assistant = ChatMessage(role = "assistant", content = "")
        conversations = conversations.map { c ->
            if (c.id != chatId) c
            else {
                val trimmed = if (c.messages.lastOrNull()?.role == "assistant") {
                    c.messages.dropLast(1)
                } else c.messages
                c.copy(updatedAt = System.currentTimeMillis(), messages = trimmed + assistant)
            }
        }
        persist()
        startStream(chatId, assistant.id)
    }

    fun stop() {
        job?.cancel()
        job = null
        client.cancel()
        val chatId = streamingChatId
        val msgId = streamingMessageId
        val text = streamText
        streamingChatId = null
        streamingMessageId = null
        streamText = ""
        if (chatId == null || msgId == null) return
        conversations = conversations.map { c ->
            if (c.id != chatId) c
            else if (text.isBlank()) c.copy(messages = c.messages.filter { it.id != msgId })
            else c.copy(
                messages = c.messages.map {
                    if (it.id == msgId) it.copy(content = text) else it
                },
            )
        }
        persist()
    }

    private fun startStream(chatId: String, messageId: String) {
        streamingChatId = chatId
        streamingMessageId = messageId
        streamText = ""
        job = viewModelScope.launch {
            try {
                val chat = conversations.find { it.id == chatId } ?: return@launch
                val history = chat.messages
                    .filter { it.id != messageId }
                    .filter { it.role == "user" || it.content.isNotBlank() }
                    .takeLast(20)
                    .map { ChatTurn(it.role, it.content.take(8000)) }
                val text = withContext(Dispatchers.IO) {
                    client.complete(settings, history, chat.route) { partial ->
                        streamText = partial
                    }
                }
                finish(chatId, messageId, text, null)
            } catch (e: Exception) {
                val cancelled = e.message?.contains("cancel", ignoreCase = true) == true
                if (cancelled) return@launch
                val msg = e.message?.ifBlank { null } ?: "Couldn't reach OmniRoute."
                finish(chatId, messageId, streamText, msg)
                toast = msg
            }
        }
    }

    private fun finish(chatId: String, messageId: String, text: String, error: String?) {
        conversations = conversations.map { c ->
            if (c.id != chatId) c
            else c.copy(
                updatedAt = System.currentTimeMillis(),
                messages = c.messages.map {
                    if (it.id == messageId) it.copy(content = text, error = error) else it
                },
            )
        }.sortedByDescending { it.updatedAt }
        streamingChatId = null
        streamingMessageId = null
        streamText = ""
        persist()
    }

    private fun persist() {
        store.save(
            ChatStore.Snapshot(
                conversations = conversations,
                settings = settings,
                defaultRoute = defaultRoute,
            ),
        )
    }

    fun consumeToast() {
        toast = null
    }

    fun newId(): String = UUID.randomUUID().toString()
}
