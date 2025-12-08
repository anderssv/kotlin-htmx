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
 * Note: LightningCSS processes standard CSS syntax (CSS custom properties, CSS nesting).
 * It does NOT support SASS-style variables ($variable). For SASS syntax, use PostCSS.
 *
 * The test file lightningcss-test.css uses standard CSS syntax that LightningCSS can process.
 * The test.scss file uses SASS syntax that only PostCSS can process.
 */
class CssLightningCssProcessingEndpointTest {
    @Test
    fun `should process CSS with LightningCSS when processor parameter is set`() =
        testApplication {
            application {
                module()
            }

            // Use the CSS file with standard CSS syntax (not SASS)
            val response = client.get("/css/lightningcss-test.css?processor=lightningcss")

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
        }

    @Test
    fun `should use LightningCSS by default when no processor parameter is set`() =
        testApplication {
            application {
                module()
            }

            // Request without processor parameter - should use LightningCSS by default
            // Use the CSS file with standard CSS syntax
            val response = client.get("/css/lightningcss-test.css")

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            println("Default processor (should be LightningCSS) test response:")
            println(responseBody)

            // Should contain processed CSS with unwrapped nested selectors
            assertTrue(
                responseBody.contains(".nested-test .child"),
                "Should process nested selectors",
            )
        }

    @Test
    fun `should use PostCSS when processor parameter is set to postcss`() =
        testApplication {
            application {
                module()
            }

            // PostCSS can handle SASS-style $variables via postcss-simple-vars plugin
            val response = client.get("/css/test.scss?processor=postcss")

            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()

            println("PostCSS processing test response:")
            println(responseBody)

            // PostCSS processes $variables to their values
            assertTrue(
                responseBody.contains("#007bff"),
                "PostCSS should process SCSS variables",
            )

            // PostCSS should process nested selectors
            assertTrue(
                responseBody.contains(".card .header"),
                "PostCSS should process nested selectors",
            )
        }
}
