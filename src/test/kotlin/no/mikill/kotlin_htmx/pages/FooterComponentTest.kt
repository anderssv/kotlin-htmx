package no.mikill.kotlin_htmx.pages

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FooterComponentTest {
    @Test
    fun `footerComponent renders footer element with copyright and links`() {
        val html =
            createHTML().div {
                footerComponent()
            }

        assertThat(html).contains("<footer")
        assertThat(html).contains("Anders Sveen")
        assertThat(html).contains("href=\"https://www.mikill.no\"")
        assertThat(html).contains("mikill.no")
    }
}
