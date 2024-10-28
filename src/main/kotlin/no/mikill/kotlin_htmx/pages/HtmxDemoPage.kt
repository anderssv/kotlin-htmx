package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import io.ktor.server.http.toHttpDateString
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*
import kotlinx.io.IOException
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.htmxSectionContent
import no.mikill.kotlin_htmx.pages.HtmlElements.respondHtmlFragment
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.util.Collections
import kotlin.text.toInt
import kotlin.time.Duration.Companion.seconds

class HtmxDemoPage {
    private val logger = LoggerFactory.getLogger(HtmxDemoPage::class.java)

    private val xDimension = 30
    private val yDimension = 100

    // Initialize a map from x and y dimension with false as the default state
    private val lookup: Array<BooleanArray> = Array(xDimension) { BooleanArray(yDimension) { false } }

    private var notify: MutableList<(suspend () -> Unit)> = Collections.synchronizedList(mutableListOf())

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

        notify.forEachIndexed { index, it ->
            try {
                it.invoke()
            } catch (e: IOException) {
                logger.info("Removing failed connection", e)
                notify.removeAt(index)
            }
        }

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

    suspend fun onCheckboxUpdate(function: suspend () -> Unit) {
        this.notify.add(function)
    }

    suspend fun renderCheckboxesPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate())) {
                headerContent {
                    div {
                        p { +"Showing: ${xDimension * yDimension} checkboxes" }
                        p { +"State is only kept in memory, so a restart of the server will blank the matrix." }
                        p { +"Update event is sent with SSE. Whole section with checkboxes is updated every time a checkbox is updated." }
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