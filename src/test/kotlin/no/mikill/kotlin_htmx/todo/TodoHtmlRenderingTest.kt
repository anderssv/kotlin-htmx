package no.mikill.kotlin_htmx.todo

import kotlinx.html.body
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TodoHtmlRenderingTest {
    private fun renderHtml(block: kotlinx.html.BODY.() -> Unit): String =
        buildString {
            appendHTML().html {
                body {
                    block(this)
                }
            }
        }

    @Test
    fun `todoListHtmlContent renders list with item titles`() {
        val items =
            listOf(
                TodoListItem(1, "Buy milk", false),
                TodoListItem(2, "Buy bread", false),
            )

        val html = renderHtml { todoListHtmlContent("test", items) }

        assertThat(html).contains("<ul")
        assertThat(html).contains("<li>Buy milk</li>")
        assertThat(html).contains("<li>Buy bread</li>")
        assertThat(html).contains("id=\"test-date\"")
    }

    @Test
    fun `htmlTodolistSectionContent renders section with todo items`() {
        val items =
            listOf(
                TodoListItem(1, "Buy milk", false),
            )

        val html = renderHtml { htmlTodolistSectionContent(items) }

        assertThat(html).contains("<section")
        assertThat(html).contains("<h1>HTML Element</h1>")
        assertThat(html).contains("<li>Buy milk</li>")
    }
}
