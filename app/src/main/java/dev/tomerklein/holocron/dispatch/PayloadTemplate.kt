package dev.tomerklein.holocron.dispatch

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Renders a destination payload. Supported placeholders (case-sensitive):
 * `{{sender}}`, `{{body}}`, `{{timestamp}}`, `{{ruleName}}`.
 *
 * With no template, [defaultJsonEnvelope] produces the canonical
 * `{sender, body, timestamp, ruleName}` JSON object.
 */
object PayloadTemplate {
    private val json = Json { encodeDefaults = true }

    fun render(template: String, message: IncomingMessage, ruleName: String): String =
        template
            .replace("{{sender}}", message.sender)
            .replace("{{body}}", message.body)
            .replace("{{timestamp}}", message.timestamp.toString())
            .replace("{{ruleName}}", ruleName)

    fun defaultJsonEnvelope(message: IncomingMessage, ruleName: String): String =
        json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "sender" to JsonPrimitive(message.sender),
                    "body" to JsonPrimitive(message.body),
                    "timestamp" to JsonPrimitive(message.timestamp),
                    "ruleName" to JsonPrimitive(ruleName),
                ),
            ),
        )
}
