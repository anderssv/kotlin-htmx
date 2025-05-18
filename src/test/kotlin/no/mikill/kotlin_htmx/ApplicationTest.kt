package no.mikill.kotlin_htmx

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            module(1000)
        }
        client.get("/select").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertThat(bodyAsText()).contains("Three")
        }
    }
}
