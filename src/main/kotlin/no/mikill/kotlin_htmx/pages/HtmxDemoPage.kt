package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.htmxSectionContent
import kotlin.time.Duration.Companion.seconds

class HtmxDemoPage {

    suspend fun renderPage(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                headerContent {
                    div {
                        +"Page header..."
                    }
                }
                mainTemplateContent {
                    demoPagesContent {
                        htmxSectionContent(5.seconds, 5.seconds)
                    }
                }
            }
        }
    }
}