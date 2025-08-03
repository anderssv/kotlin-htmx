package no.mikill.kotlin_htmx

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("/select").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertThat(bodyAsText()).contains("Three")
        }
    }
}
