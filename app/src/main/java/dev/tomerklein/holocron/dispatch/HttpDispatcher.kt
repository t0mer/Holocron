package dev.tomerklein.holocron.dispatch

import android.util.Base64
import dev.tomerklein.holocron.data.Destination
import dev.tomerklein.holocron.data.SecurePrefs
import dev.tomerklein.holocron.data.config.ApiConfig
import dev.tomerklein.holocron.data.config.AuthType
import dev.tomerklein.holocron.data.config.DestinationJson
import dev.tomerklein.holocron.data.config.WebhookConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Shared HTTP send logic for the Webhook and API destination types. */
@Singleton
class HttpSender @Inject constructor(
    private val client: OkHttpClient,
) {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    /** 2xx → Success, 5xx/timeout/IO → Retryable, 4xx → Permanent. */
    suspend fun send(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String,
    ): DispatchResult = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext DispatchResult.Permanent("Empty URL")
        val request = try {
            Request.Builder()
                .url(url)
                .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                .method(method.uppercase(), body.toRequestBody(jsonMedia))
                .build()
        } catch (e: IllegalArgumentException) {
            return@withContext DispatchResult.Permanent("Bad request: ${e.message}")
        }

        try {
            client.newCall(request).execute().use { resp ->
                when {
                    resp.isSuccessful -> DispatchResult.Success
                    resp.code in 500..599 -> DispatchResult.Retryable("HTTP ${resp.code}")
                    resp.code in 400..499 -> DispatchResult.Permanent("HTTP ${resp.code}")
                    else -> DispatchResult.Retryable("HTTP ${resp.code}")
                }
            }
        } catch (e: IOException) {
            DispatchResult.Retryable(e.message ?: "IO error")
        }
    }
}

@Singleton
class WebhookDispatcher @Inject constructor(
    private val http: HttpSender,
    private val securePrefs: SecurePrefs,
) : Dispatcher {
    override suspend fun send(message: IncomingMessage, destination: Destination): DispatchResult {
        val cfg = DestinationJson.decodeFromString(WebhookConfig.serializer(), destination.config)
        val body = cfg.bodyTemplate?.takeIf { it.isNotBlank() }
            ?.let { PayloadTemplate.render(it, message, message.ruleName) }
            ?: PayloadTemplate.defaultJsonEnvelope(message, message.ruleName)
        return http.send(cfg.url, cfg.method, headersFor(securePrefs, destination.id, cfg.authType), body)
    }
}

@Singleton
class ApiDispatcher @Inject constructor(
    private val http: HttpSender,
    private val securePrefs: SecurePrefs,
) : Dispatcher {
    override suspend fun send(message: IncomingMessage, destination: Destination): DispatchResult {
        val cfg = DestinationJson.decodeFromString(ApiConfig.serializer(), destination.config)
        val body = cfg.bodyTemplate?.takeIf { it.isNotBlank() }
            ?.let { PayloadTemplate.render(it, message, message.ruleName) }
            ?: PayloadTemplate.defaultJsonEnvelope(message, message.ruleName)
        return http.send(cfg.url, cfg.method, headersFor(securePrefs, destination.id, cfg.authType), body)
    }
}

/** SecurePrefs field keys for HTTP auth credentials, keyed per destination id. */
object HttpAuth {
    const val FIELD_USERNAME = "auth_username"
    const val FIELD_PASSWORD = "auth_password"
    const val FIELD_TOKEN = "auth_token"
    const val FIELD_CF_ID = "cf_access_client_id"
    const val FIELD_CF_SECRET = "cf_access_client_secret"
}

/** Custom headers merged with auth headers; auth headers win on key conflict. */
private fun headersFor(securePrefs: SecurePrefs, destinationId: Long, authType: AuthType): Map<String, String> =
    securePrefs.getHeaders(destinationId) + authHeaders(securePrefs, destinationId, authType)

private fun authHeaders(securePrefs: SecurePrefs, destinationId: Long, authType: AuthType): Map<String, String> {
    fun secret(field: String) = securePrefs.getSecret(destinationId, field)?.takeIf { it.isNotBlank() }
    return when (authType) {
        AuthType.NONE -> emptyMap()
        AuthType.BASIC -> {
            val user = secret(HttpAuth.FIELD_USERNAME) ?: ""
            val pass = secret(HttpAuth.FIELD_PASSWORD) ?: ""
            if (user.isEmpty() && pass.isEmpty()) {
                emptyMap()
            } else {
                val token = Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
                mapOf("Authorization" to "Basic $token")
            }
        }
        AuthType.TOKEN -> secret(HttpAuth.FIELD_TOKEN)
            ?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        AuthType.CLOUDFLARE -> buildMap {
            secret(HttpAuth.FIELD_CF_ID)?.let { put("CF-Access-Client-Id", it) }
            secret(HttpAuth.FIELD_CF_SECRET)?.let { put("CF-Access-Client-Secret", it) }
        }
    }
}
