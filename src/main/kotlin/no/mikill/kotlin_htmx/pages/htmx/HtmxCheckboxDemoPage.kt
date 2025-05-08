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

/**
 * Demo page that showcases real-time checkbox synchronization across multiple browser windows
 * using HTMX and Server-Sent Events (SSE).
 *
 * This implementation demonstrates how to:
 * - Use SSE for real-time updates without WebSockets
 * - Synchronize state across multiple clients
 * - Handle connection management and cleanup
 * - Batch updates for performance optimization
 * - Implement infinite scrolling for large datasets
 */
class HtmxCheckboxDemoPage {
    private val logger = LoggerFactory.getLogger(HtmxCheckboxDemoPage::class.java)

    private val numberOfBoxes = System.getenv("NUMBER_OF_BOXES")?.toInt() ?: 1000000
    private val initialBoxes = 2000.let { if (numberOfBoxes > it) it else numberOfBoxes / 2 }
    private val batchSize = initialBoxes / 4
    private val numberOfBatches = numberOfBoxes / batchSize

    /**
     * In-memory state storage for checkbox states.
     * In a real application, this would be replaced with a persistent database.
     * Initializes with approximately 20% of checkboxes checked.
     */
    private val checkboxState =
        BooleanArray(numberOfBoxes) { Random.nextInt(1, 10) > 8 }

    /**
     * Thread-safe list of connected SSE sessions for broadcasting updates.
     */
    private var connectedListeners: MutableList<ServerSSESession> = Collections.synchronizedList(mutableListOf())

    /**
     * Renders the complete checkbox grid HTML.
     * Uses sequences for efficient rendering of large numbers of checkboxes.
     */
    private fun HtmlBlockTag.renderBoxGridHtml() {
        div { +"Full refresh: ${ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)}" }
        div(classes = "checkbox-container") {
            // Use sequences for efficient streaming of large datasets to the client
            val initialBatches = initialBoxes / batchSize
            generateSequence(0) { it + 1 }
                .take(initialBatches)
                .forEach { batchNumber ->
                    renderBatchSpan(batchNumber)
                }

            endOfListTrigger(initialBatches - 1)
        }
    }

    /**
     * Renders a span containing a batch of checkboxes with appropriate HTMX attributes.
     */
    private fun HtmlBlockTag.renderBatchSpan(batchNumber: Int) {
        span {
            id = "batch-$batchNumber"
            attributes["sse-swap"] = "update-$batchNumber"

            renderBoxesForBatch(batchNumber)
        }
    }

    /**
     * Renders all checkboxes for a specific batch.
     */
    private fun HtmlBlockTag.renderBoxesForBatch(batchNumber: Int) {
        generateSequence(0) { it + 1 }
            .takeWhile { it <= batchSize - 1 }
            .forEach {
                val checkBoxNumber = batchNumber * batchSize + it
                renderCheckbox(checkBoxNumber, checkboxState[checkBoxNumber])
            }
    }

    /**
     * Renders a single checkbox with the appropriate HTMX attributes.
     */
    private fun HtmlBlockTag.renderCheckbox(boxNumber: Int, checkedState: Boolean) {
        input(type = InputType.checkBox) {
            attributes["hx-put"] = "checkboxes/$boxNumber"
            checked = checkedState
            id = "$boxNumber"
        }
    }

    /**
     * Renders the next batch of checkboxes for infinite scrolling.
     */
    suspend fun renderBoxBatch(context: RoutingContext) {
        val batchNumber = context.call.pathParameters["batchNumber"]!!.toInt()

        context.call.respondHtmlFragment {
            // Render the current batch
            renderBatchSpan(batchNumber)

            // Add new sentinel element if there are more batches
            if (batchNumber < numberOfBatches - 1) {
                endOfListTrigger(batchNumber)
            }
        }
    }

    private fun HtmlBlockTag.endOfListTrigger(batchNumber: Int) {
        span {
            id = "end-of-list"
            attributes["hx-get"] = "checkboxes/batch/${batchNumber + 1}"
            attributes["hx-trigger"] = "revealed"
            attributes["hx-swap"] = "outerHTML"
        }
    }

    /**
     * Broadcasts an update for a specific batch to all connected clients.
     * Also handles cleanup of dead connections.
     */
    private suspend fun broadcastUpdate(batchNumber: Int) {
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
    }

    /**
     * Renders the complete checkboxes demo page with header information and the checkbox grid.
     */
    suspend fun renderCheckboxesPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "HTMX + SSE Checkboxes demo")) {
                headerContent {
                    section {
                        img(src = "https://api.qrserver.com/v1/create-qr-code/?data=https://kotlin-htmx.fly.dev/demo/htmx/checkboxes&amp;size=200x200") {
                            style = "float: right; margin-left: 20px;"
                            classes = setOf("qr-code-image")
                            width = "200"
                            height = "200"
                        }
                        p {
                            +"This page shows synchronization between browser windows. "
                            strong { +"Open multiple windows to this URL to see it in action." }
                        }
                        p {
                            a(href = "https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/") { +"Go here for a lengthy blogpost about the implementation" }
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

    /**
     * Renders just the grid of checkboxes as an HTML fragment.
     * Used for full refreshes of the checkbox grid.
     */
    suspend fun renderBoxGridFragment(context: RoutingContext) {
        with(context) {
            call.respondHtmlFragment {
                renderBoxGridHtml()
            }
        }
    }

    /**
     * Handles a checkbox toggle request, updates the state, and broadcasts the change to all connected clients.
     */
    suspend fun handleCheckboxToggle(context: RoutingContext) {
        val boxNumber = context.call.pathParameters["boxNumber"]!!.toInt()
        checkboxState[boxNumber] = !checkboxState[boxNumber]
        val batchNumber = boxNumber / batchSize

        // Broadcast the update to all connected clients
        broadcastUpdate(batchNumber)

        // Respond to the original request with the updated checkbox
        context.call.respondHtmlFragment {
            renderCheckbox(boxNumber, checkboxState[boxNumber])
        }
    }

    /**
     * Registers a new SSE session for checkbox update notifications.
     */
    fun registerOnCheckBoxNotification(session: ServerSSESession) {
        connectedListeners.add(session)
    }

    /**
     * Unregisters an SSE session when the connection is closed.
     */
    fun unregisterOnCheckBoxNotification(session: ServerSSESession) {
        this.connectedListeners.remove(session)
    }
}