package online.omniroute.chat.data

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

class OpenAiClient {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Volatile
    private var activeCall: Call? = null

    fun cancel() {
        activeCall?.cancel()
        activeCall = null
    }

    fun complete(
        settings: ProviderSettings,
        messages: List<ChatTurn>,
        route: RouteId,
        onDelta: (String) -> Unit,
    ): String {
        val packed = listOf(ChatTurn("system", route.system)) + messages
        val custom = settings.mode == ProviderSettings.MODE_CUSTOM && settings.baseUrl.isNotBlank()
        if (custom) {
            try {
                return streamOpenAi(
                    baseUrl = settings.baseUrl,
                    apiKey = settings.apiKey,
                    model = settings.model.ifBlank { "auto" },
                    messages = packed,
                    temperature = route.temperature,
                    onDelta = onDelta,
                )
            } catch (e: Exception) {
                if (isCancel(e)) throw e
            }
        }
        return pollinations(packed, route.temperature, onDelta)
    }

    /**
     * GET {baseUrl}/models against an OpenAI-compatible endpoint (OmniRoute's
     * /v1/models route included) and return the list of model ids.
     * Throws IOException with a humanError() message on non-2xx responses,
     * reusing the same 401/403/404/429/5xx mapping as chat completions.
     */
    fun fetchModels(baseUrl: String, apiKey: String): List<String> {
        val builder = Request.Builder()
            .url(modelsUrl(baseUrl))
            .get()
            .header("Accept", "application/json")
        if (apiKey.isNotBlank()) builder.header("Authorization", "Bearer $apiKey")
        val call = http.newCall(builder.build())
        activeCall = call
        call.execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                throw IOException(humanError(response.code, err))
            }
            val payload = response.body?.string().orEmpty()
            return parseModelIds(payload)
        }
    }

    private fun modelsUrl(base: String): String {
        val t = base.trim().trimEnd('/')
        return if (t.endsWith("/models")) t else "$t/models"
    }

    private fun parseModelIds(payload: String): List<String> {
        return try {
            val json = JSONObject(payload)
            val data = json.optJSONArray("data") ?: return emptyList()
            (0 until data.length()).mapNotNull { i ->
                data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }
            }.distinct().sorted()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun streamOpenAi(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatTurn>,
        temperature: Double,
        onDelta: (String) -> Unit,
    ): String {
        val body = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put("temperature", temperature)
            .put("max_tokens", 1536)
            .put("messages", messagesJson(messages))
        val builder = Request.Builder()
            .url(completionsUrl(baseUrl))
            .post(body.toString().toRequestBody(jsonType))
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
        if (apiKey.isNotBlank()) builder.header("Authorization", "Bearer $apiKey")
        val call = http.newCall(builder.build())
        activeCall = call
        call.execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                throw IOException(humanError(response.code, err))
            }
            val stream = response.body?.byteStream() ?: throw IOException("Empty body")
            val acc = StringBuilder()
            BufferedReader(stream.reader()).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (!trimmed.startsWith("data:")) continue
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val token = extractDelta(data) ?: continue
                    acc.append(token)
                    onDelta(acc.toString())
                }
            }
            if (acc.isBlank()) throw IOException("Empty model reply")
            return acc.toString()
        }
    }

    private fun pollinations(
        messages: List<ChatTurn>,
        temperature: Double,
        onDelta: (String) -> Unit,
    ): String {
        val endpoints = listOf(
            "https://text.pollinations.ai/openai",
            "https://gen.pollinations.ai/v1/chat/completions",
        )
        var lastError = "Every route is busy right now. Try again in a moment."
        for (url in endpoints) {
            try {
                val body = JSONObject()
                    .put("model", "openai")
                    .put("stream", false)
                    .put("temperature", temperature)
                    .put("max_tokens", 1536)
                    .put("messages", messagesJson(messages))
                val req = Request.Builder()
                    .url(url)
                    .post(body.toString().toRequestBody(jsonType))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/128.0.0.0 Mobile Safari/537.36",
                    )
                    .header("Origin", "https://pollinations.ai")
                    .header("Referer", "https://pollinations.ai/")
                    .build()
                val call = http.newCall(req)
                activeCall = call
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = humanError(response.code, response.body?.string().orEmpty())
                        return@use
                    }
                    val payload = response.body?.string().orEmpty()
                    val text = extractContent(payload)
                    if (text.isBlank()) {
                        lastError = "Empty model reply"
                        return@use
                    }
                    onDelta(text)
                    return text
                }
            } catch (e: Exception) {
                if (isCancel(e)) throw e
                lastError = e.message ?: lastError
            }
        }
        throw IOException(lastError)
    }

    private fun messagesJson(messages: List<ChatTurn>): JSONArray {
        val arr = JSONArray()
        messages.forEach { turn ->
            arr.put(JSONObject().put("role", turn.role).put("content", turn.content))
        }
        return arr
    }

    private fun completionsUrl(base: String): String {
        val t = base.trim().trimEnd('/')
        return if (t.endsWith("/chat/completions")) t else "$t/chat/completions"
    }

    private fun extractDelta(data: String): String? {
        return try {
            val json = JSONObject(data)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val c0 = choices.getJSONObject(0)
            val delta = c0.optJSONObject("delta")
            val token = when {
                delta != null -> delta.optString("content", "")
                else -> c0.optJSONObject("message")?.optString("content").orEmpty()
            }
            token.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractContent(payload: String): String {
        return try {
            val json = JSONObject(payload)
            val choices = json.optJSONArray("choices") ?: return payload.trim()
            if (choices.length() == 0) return ""
            val c0 = choices.getJSONObject(0)
            val content = c0.optJSONObject("message")?.optString("content")
                ?: c0.optString("text")
            content.trim()
        } catch (_: Exception) {
            payload.trim()
        }
    }

    private fun humanError(code: Int, body: String): String {
        val snippet = body.trim().take(180)
        return when (code) {
            401, 403 -> "That key was rejected. Check the API key in Settings."
            404 -> "Endpoint not found. Check the base URL."
            429 -> "The route is rate-limited. Try again in a moment."
            in 500..599 -> "The gateway is busy ($code). Try again shortly."
            else -> if (snippet.isBlank()) "Couldn't reach OmniRoute ($code)" else snippet
        }
    }

    private fun isCancel(e: Exception): Boolean =
        e is InterruptedIOException || e.message?.contains("cancel", ignoreCase = true) == true
}
