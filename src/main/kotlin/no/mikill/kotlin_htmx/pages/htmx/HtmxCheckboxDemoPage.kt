package no.mikill.kotlin_htmx.pages.htmx

import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.html.*
import kotlinx.io.IOException
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.partialHtml
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.MainTemplate
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.MutableList
import kotlin.collections.forEach
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.random.Random

class HtmxCheckboxDemoPage {
    private val logger = LoggerFactory.getLogger(HtmxCheckboxDemoPage::class.java)

    private val numberOfBoxes = 5000
    private val checkboxState =
        BooleanArray(numberOfBoxes) { Random.nextInt(1, 10) > 8 } // This is our "DB". Initializing 20% filled.
    private var connectedListeners: MutableList<ServerSSESession> = Collections.synchronizedList(mutableListOf())

    suspend fun renderBoxGridFragment(context: RoutingContext) {
        with(context) {
            call.respondHtmlFragment {
                div {
                    renderBoxGridHtml()
                }
            }
        }
    }

    suspend fun renderCheckboxesPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "HTMX + SSE Checkboxes demo")) {
                headerContent {
                    section {
                        p {
                            +"This page shows synchronization between browser windows. "
                            strong { +"Open multiple windows to this URL to see it in action."}
                        }
                        p {
                            a(href="https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/") {+"Go here for a lengty blogpost about the implementation"}
                            + " and links to code. I use HTMX, SSE and KTor to do this."
                        }
                        p {
                            +"It is inspired by "
                            a(href = "https://hamy.xyz/labs/1000-checkboxes") {
                                +"Hamilton Greene's 1000-checkboxes"
                            }
                        }
                    }
                    section {
                        p { +"Showing: $numberOfBoxes checkboxes." }
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        div {
                            style = "max-width: 40em;"
                            attributes["hx-ext"] = "sse"
                            attributes["sse-connect"] = "checkboxes/events"
                            div {
                                attributes["hx-get"] = "checkboxes/all"
                                attributes["hx-trigger"] = "sse:update-all"

                                renderBoxGridHtml()
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun handleCheckboxToggle(context: RoutingContext) {
        val boxNumber = context.call.pathParameters["boxNumber"]!!.toInt()
        checkboxState[boxNumber] = !checkboxState[boxNumber]

        /**
         * These are registered, but there doesn't seem to be a hook
         * for closing connections. So we handle that when we iterate
         * through the list and remove the broken ones.
         */
        val iterator = connectedListeners.iterator()
        while (iterator.hasNext()) {
            try {
                iterator.next().send(
                    partialHtml {
                        renderCheckbox(boxNumber, checkboxState[boxNumber])
                    },
                    "update-$boxNumber",
                    UUID.randomUUID().toString()
                )
            } catch (e: IOException) {
                logger.info("Dead connection detected, unregistering", e)
                iterator.remove()
            }
        }

        context.call.respondHtmlFragment {
            renderCheckbox(boxNumber, checkboxState[boxNumber])
        }
    }

    fun registerOnCheckBoxNotification(session: ServerSSESession) {
        connectedListeners.add(session)
    }

    fun unregisterOnCheckBoxNotification(session: ServerSSESession) {
        this.connectedListeners.remove(session)
    }

    private fun DIV.renderBoxGridHtml() {
        div { +"Full refresh: ${ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)}" }
        div {
            (0..numberOfBoxes - 1).forEach { boxNumber ->
                span {
                    /*
                     * There are pros and cons to have SSE and PUT on the same element.
                     * It basically means that you will get two DOM updates, one for the
                     * response from the PUT and one from the SSE Event. But it shouldn't
                     * be noticeable in this case.
                     */
                    attributes["sse-swap"] = "update-${boxNumber}" // Takes the HTML from the message and inserts
                    attributes["hx-put"] = "checkboxes/$boxNumber"

                    renderCheckbox(boxNumber, checkboxState[boxNumber])
                }
            }
        }
    }

    private fun HtmlBlockTag.renderCheckbox(boxNumber: Int, checkedState: Boolean) {
        input(type = InputType.checkBox) {
            checked = checkedState
            id = "$boxNumber"
        }
    }

}