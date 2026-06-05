package dev.tomerklein.holocron.dispatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadTemplateTest {

    private val message = IncomingMessage(
        sender = "+972501234567",
        body = "Your code is 1234",
        timestamp = 1_700_000_000_000,
        defaultRegion = "IL",
        ruleName = "Bank alerts",
    )

    @Test
    fun render_replacesAllPlaceholders() {
        val template = "from={{sender}} text={{body}} ts={{timestamp}} rule={{ruleName}}"
        val result = PayloadTemplate.render(template, message, message.ruleName)
        assertEquals(
            "from=+972501234567 text=Your code is 1234 ts=1700000000000 rule=Bank alerts",
            result,
        )
    }

    @Test
    fun defaultEnvelope_isValidJsonWithAllFields() {
        val json = PayloadTemplate.defaultJsonEnvelope(message, message.ruleName)
        assertTrue(json.contains("\"sender\":\"+972501234567\""))
        assertTrue(json.contains("\"body\":\"Your code is 1234\""))
        assertTrue(json.contains("\"timestamp\":1700000000000"))
        assertTrue(json.contains("\"ruleName\":\"Bank alerts\""))
    }
}
