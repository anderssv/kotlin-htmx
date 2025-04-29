package no.mikill.kotlin_htmx.todo

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlElements.htmlTodolistSectionContent
import no.mikill.kotlin_htmx.pages.MainTemplate

class HtmlTodoDemoPage {

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
                        htmlTodolistSectionContent()
                    }
                }
            }
        }
    }
}