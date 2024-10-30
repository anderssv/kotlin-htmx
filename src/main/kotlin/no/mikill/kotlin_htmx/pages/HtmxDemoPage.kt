package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*
import kotlinx.io.IOException
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.htmxSectionContent
import no.mikill.kotlin_htmx.pages.HtmlElements.respondHtmlFragment
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import kotlin.text.toInt
import kotlin.time.Duration.Companion.seconds

class HtmxDemoPage {
    private val logger = LoggerFactory.getLogger(HtmxDemoPage::class.java)

    private val numberOfBoxes = 3000

    // Initialize a map from x and y dimension with false as the default state
    private val checkboxState = BooleanArray(numberOfBoxes) { false }

    private var connectedListeners: MutableList<(suspend (boxNum: Int, checkedState: Boolean) -> Unit)> =
        Collections.synchronizedList(mutableListOf())

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
        val boxNummber = context.call.pathParameters["boxNumber"]!!.toInt()
        checkboxState[boxNummber] = !checkboxState[boxNummber]

        /**
         * These are registered, but there doesn't seem to be a hook
         * for closing connections. So we handle that when we iterate
         * through the list, and remove the broken ones.
         */
        val iterator = connectedListeners.iterator()
        while (iterator.hasNext()) {
            try {
                iterator.next().invoke(boxNummber, checkboxState[boxNummber])
            } catch (e: IOException) {
                logger.info("Removing failed connection", e)
                iterator.remove()
            }
        }

        context.call.respondText("Ok")
    }

    suspend fun boxGridFragment(context: RoutingContext) {
        with(context) {
            call.respondHtmlFragment {
                div {
                    boxGridHtml()
                }
            }
        }
    }

    fun onCheckboxUpdate(function: suspend (boxNum: Int, checkedState: Boolean) -> Unit) {
        this.connectedListeners.add(function)
    }

    suspend fun renderCheckboxesPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate())) {
                headerContent {
                    div {
                        p { +"Showing: ${numberOfBoxes} checkboxes." }
                        p { +"This page shows how you can do a event driven synchronization between browsers with HTMX and SSE. Open an additional browser to see updates between them. State is only kept in memory, so a restart of the server will wipe the matrix." }
                        p {
                            +"Some notes:"
                            ul {
                                li {
                                    +"Updates are partial per checkbox. This creates a blind spot if you loose or have intermittent connections. When the page reconnects to the SSE endpoint, the whole checkbox matrix will be reloaded, which can be slow. One possible fix could be to split the matrix into parts."
                                }
                                li {
                                    +"There is no proper error handling, but the SSE stream will be re-connected, possibly overwriting changes you have done locally."
                                }
                                li {
                                    +"When the number of checkboxes becomes large rendering gets slow, but also the processing (HTMX scans for things to do)."
                                }
                                li {
                                    +"Update event is sent with "
                                    a(href = "https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events") { +"SSE" }
                                    +" (see events endpoint in developer console), with the fresh SSE support in KTor. Whole div with checkboxes is updated every time a checkbox is updated (update endpoint). HTMX listens for SSE events and triggers an update of the HTML. "
                                    +"You can view most of the code needed for this "
                                    a(href = "https://github.com/anderssv/kotlin-htmx/blob/main/src/main/kotlin/no/mikill/kotlin_htmx/pages/HtmxDemoPage.kt") { +"here" }
                                    +"."
                                }
                            }
                        }
                        p {
                            +"This is inspired by "
                            a(href = "https://hamy.xyz/labs/1000-checkboxes") {
                                +"Hamilton Greene's 1000-checkboxes"
                            }
                            +". I wanted to see how I could do it with SSE and how instant updates felt. He also has another solution that handles a million checkboxes."
                        }
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        div {
                            attributes["hx-ext"] = "sse"
                            attributes["sse-connect"] = "checkboxes/events"
                            div {
                                attributes["hx-get"] = "checkboxes/update"
                                attributes["hx-trigger"] = "sse:update-all"

                                boxGridHtml()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DIV.boxGridHtml() {
        div { +"Full refresh: ${ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)}" }
        div {
            (0..numberOfBoxes - 1).forEach { boxNumber ->
                checkbox(boxNumber, checkboxState[boxNumber])
            }
        }
    }

}

fun HtmlBlockTag.checkbox(boxNumber: Int, checkedState: Boolean) {
    span {
        attributes["hx-sse"] = "swap:update-${boxNumber}" // Takes the HTML from the message and inserts
        input(type = InputType.checkBox) {
            attributes["hx-put"] = "checkboxes/$boxNumber"
            checked = checkedState
            id = "$boxNumber"
        }
    }
}
