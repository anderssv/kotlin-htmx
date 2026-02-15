package no.mikill.kotlin_htmx.css

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.mikill.kotlin_htmx.module
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for LightningCSS processing.
 *
 * LightningCSS processes standard CSS syntax including CSS custom properties and CSS nesting.
 * The test file lightningcss-test.css uses standard CSS syntax that LightningCSS can process.
 */
class CssLightningCssProcessingEndpointTest {
    @Test
    fun `should process CSS with LightningCSS`() =
        testApplication {
            application {
                module()
            }

            val response = client.get("/css/lightningcss-test.css")

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            println("LightningCSS processing test response:")
            println(responseBody)

            // LightningCSS should unwrap nested selectors
            assertTrue(
                responseBody.contains(".card .header"),
                "Should process nested selectors",
            )

            // Should preserve CSS custom properties
            assertTrue(
                responseBody.contains("--primary-color"),
                "Should preserve CSS custom properties",
            )

            // Should contain processed CSS with unwrapped nested selectors
            assertTrue(
                responseBody.contains(".nested-test .child"),
                "Should process nested selectors",
            )
        }
}
