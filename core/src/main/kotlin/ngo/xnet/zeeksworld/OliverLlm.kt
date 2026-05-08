package ngo.xnet.zeeksworld

import java.net.HttpURLConnection
import java.net.URI

object OliverLlm {
    private const val API_URL = "https://inf.xnet.ngo/v1/chat/completions"
    private var apiKey = BuildConfig.LLM_API_KEY
    private const val MODEL = "pollinations-pollen/gemini-fast"
    private const val SYSTEM_PROMPT = "You are Oliver, a 1-year-old orange tabby cat. You speak simply so a 5-year-old can understand. You mix in cat sounds like meow, purr, mrrp. Keep responses to 1-2 short sentences."

    fun setApiKey(key: String) { apiKey = key }

    fun generateResponse(event: String, callback: (String) -> Unit) {
        Thread {
            try {
                val body = """{"model":"$MODEL","messages":[{"role":"system","content":"$SYSTEM_PROMPT"},{"role":"user","content":"${event.replace("\"", "\\\"")}"}],"max_tokens":60}"""
                val conn = URI(API_URL).toURL().openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 10000
                conn.outputStream.use { it.write(body.toByteArray()) }
                val resp = conn.inputStream.bufferedReader().readText()
                val content = Regex(""""content"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(resp)?.groupValues?.get(1) ?: "Meow!"
                callback(content.replace("\\n", "\n").replace("\\\"", "\""))
            } catch (e: Exception) {
                callback("Mrrp! *stretches*")
            }
        }.start()
    }
}
