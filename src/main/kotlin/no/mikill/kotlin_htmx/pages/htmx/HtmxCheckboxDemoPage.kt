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
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

class HtmxCheckboxDemoPage {
    private val logger = LoggerFactory.getLogger(HtmxCheckboxDemoPage::class.java)

    private val batchSize = 1
    private val numberOfBoxes = System.getenv("NUMBER_OF_BOXES")?.toInt() ?: 10000
    private val checkboxState =
        BooleanArray(numberOfBoxes) { Random.nextInt(1, 10) > 8 } // This is our "DB". Initializing 20% filled.
    private var connectedListeners: MutableList<ServerSSESession> = Collections.synchronizedList(mutableListOf())

    suspend fun renderBoxGridFragment(context: RoutingContext) {
        with(context) {
            call.respondHtmlFragment {
                renderBoxGridHtml()
            }
        }
    }

    suspend fun renderCheckboxesPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "HTMX + SSE Checkboxes demo")) {
                headerContent {
                    section {
                        img(src = "https://api.qrserver.com/v1/create-qr-code/?data=https://kotlin-htmx.fly.dev/demo/htmx/checkboxes&amp;size=200x200") {
                            style = "float: right; margin-left: 20px;"
                            classes = setOf("qr-code-image")
                        }
                        p {
                            +"This page shows synchronization between browser windows. "
                            strong { +"Open multiple windows to this URL to see it in action." }
                        }
                        p {
                            a(href = "https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/") { +"Go here for a lengty blogpost about the implementation" }
                            +" and links to code. I use HTMX, SSE and KTor to do this. "
                            +"It is inspired by "
                            a(href = "https://hamy.xyz/labs/1000-checkboxes") {
                                +"Hamilton Greene's 1000-checkboxes"
                            }
                        }
                        p {
                            +"Showing: ${
                                NumberFormat.getNumberInstance(Locale.forLanguageTag("no-bok")).format(numberOfBoxes)
                            } checkboxes. SSE update batches are $batchSize. "
                        }
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        div {
                            style = "max-width: 40em;"
                            attributes["hx-ext"] = "sse"
                            attributes["sse-connect"] = "checkboxes/events"
                            section {
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
        val batchNumber = boxNumber / batchSize

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
                        renderBoxesForBatch(batchNumber)
                    },
                    "update-$batchNumber",
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

    private fun HtmlBlockTag.renderBoxGridHtml() {
        div { +"Full refresh: ${ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)}" }
        div {
            // If the number is high it is really imporant to have this as a sequence to start
            // sending data to the client and not wait for the whole thing to be done.
            generateSequence(0) { it + 1 }
                .takeWhile { it <= (numberOfBoxes / batchSize) - 1 }
                .forEach { batchNumber ->
                    span {
                        /*
                         * There are pros and cons to have SSE and PUT on the same element.
                         * It basically means that you will get two DOM updates, one for the
                         * response from the PUT and one from the SSE Event. But it shouldn't
                         * be noticeable in this case.
                         */
                        attributes["sse-swap"] = "update-${batchNumber}" // Takes the HTML from the message and inserts

                        renderBoxesForBatch(batchNumber)
                    }
                }
        }
    }

    private fun HtmlBlockTag.renderBoxesForBatch(batchNumber: Int) {
        generateSequence(0) { it + 1 }
            .takeWhile { it <= batchSize - 1 }
            .forEach {
                val checkBoxNumber = batchNumber * batchSize + it
                renderCheckbox(checkBoxNumber, checkboxState[checkBoxNumber])
            }
    }

    private fun HtmlBlockTag.renderCheckbox(boxNumber: Int, checkedState: Boolean) {
        input(type = InputType.checkBox) {
            attributes["hx-put"] = "checkboxes/$boxNumber"

            checked = checkedState
            id = "$boxNumber"
        }
    }

}
