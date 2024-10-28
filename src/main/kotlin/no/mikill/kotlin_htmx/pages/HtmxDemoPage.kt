package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import io.ktor.server.http.toHttpDateString
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.htmxSectionContent
import no.mikill.kotlin_htmx.pages.HtmlElements.respondHtmlFragment
import java.time.ZonedDateTime
import kotlin.text.toInt
import kotlin.time.Duration.Companion.seconds

class HtmxDemoPage {
    private val xDimension = 20
    private val yDimension = 50

    // Initialize a map from x and y dimension with false as the default state
    val lookup: Array<BooleanArray> = Array(xDimension) { BooleanArray(yDimension) { false } }

    suspend fun renderPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate())) {
                headerContent {
                    div {
                        +"Page header"
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        htmxSectionContent(
                            loadDelay = 5.seconds,
                            backendDelay = 5.seconds
                        )
                    }
                }
            }
        }
    }

    suspend fun toggle(context: RoutingContext) {
        val x = context.call.pathParameters["x"]!!.toInt()
        val y = context.call.pathParameters["y"]!!.toInt()
        lookup[x][y] = !lookup[x][y]

        context.call.response.header("HX-Redirect", "/demo/htmx/checkboxes")
        context.call.respondText("Ok")
    }

    suspend fun boxes(context: RoutingContext) {
        with(context) {
            call.respondHtmlFragment {
                div {
                    justBoxes()
                }
            }
        }
    }

    suspend fun renderCheckboxesPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate())) {
                headerContent {
                    div {
                        +"Page header!"
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        div {
                            attributes["hx-ext"] = "sse"
                            attributes["sse-connect"] = "checkboxes/events"
                            div {
                                attributes["hx-get"] = "checkboxes/update"
                                attributes["hx-trigger"] = "sse:checkbox"

                                justBoxes()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.justBoxes() {
        div { +ZonedDateTime.now().toHttpDateString() }
        (0..yDimension - 1).forEach { rowCtr ->
            div {
                (0..xDimension - 1).forEach { columnCtr ->
                    input(type = InputType.checkBox) {
                        attributes["hx-put"] = "checkboxes/$columnCtr/$rowCtr"
                        checked = lookup[columnCtr][rowCtr]
                        id = "${rowCtr}-${columnCtr}"
                    }
                }
            }
        }
    }
}