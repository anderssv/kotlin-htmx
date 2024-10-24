package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.htmxSectionContent
import kotlin.time.Duration.Companion.seconds

class HtmxDemoPage {

    suspend fun renderPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                headerContent {
                    div {
                        +"Page header..."
                    }
                }
                mainTemplateContent {
                    demoPagesContent {
                        htmxSectionContent(
                            loadDelay = 5.seconds,
                            backendDelay = 5.seconds
                        )
                    }
                }
            }
        }
    }
}