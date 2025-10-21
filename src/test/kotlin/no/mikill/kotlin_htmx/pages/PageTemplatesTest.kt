package no.mikill.kotlin_htmx.pages

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.mikill.kotlin_htmx.module
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PageTemplatesTest {
    @Test
    fun `MainTemplate should use headerComponent and footerComponent`() =
        testApplication {
            application {
                module()
            }
            client.get("/").apply {
                assertEquals(HttpStatusCode.OK, status)
                val html = bodyAsText()
                assertThat(html).contains("class=\"site-header\"")
                assertThat(html).contains("Kotlin, KTor and HTMX front end demos")
                assertThat(html).contains("Anders Sveen")
                assertThat(html).contains("mikill.no")
            }
        }
}
