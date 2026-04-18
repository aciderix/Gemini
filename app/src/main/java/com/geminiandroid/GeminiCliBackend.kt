package com.geminiandroid

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GeminiCliBackend(
    private val client: OkHttpClient = OkHttpClient(),
    private var baseUrl: String = "http://10.0.2.2:8765"
) {
    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun login(accountToken: String): String =
        postJson("/v1/account/login", JSONObject().put("token", accountToken).toString())

    fun accountStatus(): String =
        postJson("/v1/account/status", "{}")

    fun commands(): String =
        postJson("/v1/cli/commands", "{}")

    fun send(input: String, cwd: String? = null): String {
        val payload = JSONObject()
            .put("input", input)
        if (!cwd.isNullOrBlank()) payload.put("cwd", cwd)

        return postJson("/v1/cli/execute", payload.toString())
    }

    private fun postJson(path: String, json: String): String {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            return if (response.isSuccessful) body else "Backend error ${response.code}: $body"
        }
    }
}
