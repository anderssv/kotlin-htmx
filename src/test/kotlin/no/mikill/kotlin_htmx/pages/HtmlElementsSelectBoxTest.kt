package no.mikill.kotlin_htmx.pages

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HtmlElementsSelectBoxTest {
    @Test
    fun `selectBox renders a boosted link that animates with a view transition`() {
        val html =
            createHTML().div {
                with(HtmlElements) {
                    selectBox(name = "One", linkUrl = "/select/One", imageUrl = "/img/one.png")
                }
            }

        assertThat(html).contains("href=\"/select/One\"")
        assertThat(html).contains("hx-boost=\"true\"")
        // htmx 4: the per-swap `transition:true` modifier opts this navigation into the browser
        // View Transitions API without enabling transitions globally (which would animate the
        // high-frequency checkbox SSE updates too).
        assertThat(html).contains("hx-swap=\"outerHTML show:window:top transition:true\"")
    }
}
