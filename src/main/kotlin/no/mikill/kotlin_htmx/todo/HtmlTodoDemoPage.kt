package no.mikill.kotlin_htmx.todo

import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.routing.RoutingContext
import kotlinx.html.p
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlElements.htmlTodolistSectionContent
import no.mikill.kotlin_htmx.pages.MainTemplate

/**
 * Demo page that showcases a simple todolist implementation using pure HTML.
 *
 * This class demonstrates how to create a basic todolist page
 * using server-side rendering with no JavaScript. It serves as a
 * baseline for comparison with other implementations (HTMX, React, etc.).
 */
class HtmlTodoDemoPage {

    /**
     * Renders the HTML todolist page with header and main content.
     *
     * @param context The routing context for the request
     */
    suspend fun renderHtmlPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Multi frameworks demo")) {
                headerContent {
                    p {
                        +"Pure HTML view of the todo list"
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        // Render the todolist section with pure HTML
                        // This uses a shared component from HtmlElements to maintain consistency
                        // across different implementations while demonstrating server-side rendering
                        // todoListItems would normally be feched from a database
                        htmlTodolistSectionContent(todoListItems)
                    }
                }
            }
        }
    }
}
