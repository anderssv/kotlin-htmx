package no.mikill.kotlin_htmx.pages.htmx

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.div
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlElements.htmxTodolistSectionContent
import no.mikill.kotlin_htmx.pages.MainTemplate
import kotlin.time.Duration.Companion.seconds

/**
 * Demo page that showcases a todolist implementation using HTMX.
 *
 * This class demonstrates how to create a simple todolist page
 * that uses HTMX for dynamic content loading and interaction.
 */
class HtmxTodolistDemoPage {

    /**
     * Renders the HTMX todolist page with header and main content.
     *
     * @param context The routing context for the request
     */
    suspend fun renderHtmxTodoListPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "HTMX TodoList Demo")) {
                headerContent {
                    div {
                        +"Page header"
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        // Render the todolist section with HTMX functionality
                        // - loadDelay: null means no client-side delay for initial load
                        // - backendDelay: 5 seconds simulated delay on the backend to demonstrate loading states
                        htmxTodolistSectionContent(
                            loadDelay = null,
                            backendDelay = 5.seconds
                        )
                    }
                }
            }
        }
    }

}
