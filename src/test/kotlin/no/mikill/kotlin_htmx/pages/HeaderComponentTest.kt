package no.mikill.kotlin_htmx.pages

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HeaderComponentTest {
    @Test
    fun `headerComponent renders header element with site title and navigation`() {
        val html =
            createHTML().div {
                headerComponent()
            }

        assertThat(html).contains("<header")
        assertThat(html).contains("Kotlin, KTor and HTMX front end demos")
        assertThat(html).contains("<nav")
        assertThat(html).contains("href=\"/\"")
        assertThat(html).contains("href=\"/demo/htmx/checkboxes\"")
        assertThat(html).contains("href=\"/person/register\"")
    }
}
