package no.mikill.kotlin_htmx.pages.htmx

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.div
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlElements.htmxTodolistSectionContent
import no.mikill.kotlin_htmx.pages.MainTemplate
import kotlin.time.Duration.Companion.seconds

class HtmxTodolistDemoPage {

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
                        htmxTodolistSectionContent(
                            loadDelay = 5.seconds,
                            backendDelay = 5.seconds
                        )
                    }
                }
            }
        }
    }

}