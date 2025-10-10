@file:OptIn(ExperimentalKtorApi::class)

package no.mikill.kotlin_htmx.pages.htmx

import io.ktor.htmx.HxSwap
import io.ktor.htmx.html.hx
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sse.ServerSSESession
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.html.HtmlBlockTag
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.input
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.strong
import kotlinx.html.style
import kotlinx.html.unsafe
import kotlinx.io.IOException
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.partialHtml
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.MainTemplate
import org.slf4j.LoggerFactory
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import java.util.UUID
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
class HtmxCheckboxDemoPage(
    val numberOfBoxes: Int,
) {
    private val logger = LoggerFactory.getLogger(HtmxCheckboxDemoPage::class.java)

    private val initialBoxes = 2000.let { if (numberOfBoxes > it) it else numberOfBoxes / 2 }
    private val batchSize = initialBoxes / 6
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

        // Add the connected browsers counter at the top
        div {
            id = "client-counter"
            attributes["sse-swap"] = "update-client-count"
            span {
                +"Connected browsers: ${connectedListeners.size}"
            }
        }

        // Add the checkbox counter that will always be visible
        div(classes = "checkbox-counter") {
            id = "checkbox-counter"
            p { +"Max boxes: $numberOfBoxes" }
            p { +"Batch size: $batchSize" }
            p {
                id = "loaded-counter"
                +"Loaded in browser: 0"
            }
        }

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

            /**
             * The sse-swap attribute tells HTMX to replace this element's content
             * when an SSE event with the specified name is received.
             *
             * Note: Having both SSE updates and PUT responses on the same element
             * means we'll get two DOM updates (one from the PUT response and one
             * from the SSE event), but the visual effect is negligible in this case.
             */
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
    private fun HtmlBlockTag.renderCheckbox(
        boxNumber: Int,
        checkedState: Boolean,
    ) {
        input(type = InputType.checkBox) {
            // NOT-IN-DSL: . Put not in DSL.
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
            attributes.hx {
                get = "checkboxes/batch/${batchNumber + 1}"
                trigger = "revealed"
                swap = HxSwap.outerHtml
            }
        }
    }

    /**
     * Broadcasts an update for a specific batch to all connected clients.
     * Also handles cleanup of dead connections.
     */
    private suspend fun broadcastUpdate(batchNumber: Int) {
        var deadConnectionRemoved = false
        val iterator = connectedListeners.iterator()
        while (iterator.hasNext()) {
            try {
                iterator.next().send(
                    partialHtml {
                        renderBoxesForBatch(batchNumber)
                    },
                    "update-$batchNumber",
                    UUID.randomUUID().toString(),
                )
            } catch (e: Exception) {
                logger.info("Dead connection detected, unregistering", e)
                iterator.remove()
                deadConnectionRemoved = true
            }
        }

        // Broadcast updated client count if any dead connections were removed
        if (deadConnectionRemoved) {
            broadcastClientCount()
        }
    }

    /**
     * Broadcasts the current client count to all connected clients.
     * Removes dead connections and re-broadcasts if any are found.
     */
    private suspend fun broadcastClientCount() {
        val count = connectedListeners.size
        var deadConnectionRemoved = false

        val iterator = connectedListeners.iterator()
        while (iterator.hasNext()) {
            val session = iterator.next()
            try {
                val html =
                    partialHtml {
                        span {
                            +"Connected browsers: $count"
                        }
                    }
                session.send(
                    html,
                    "update-client-count",
                    UUID.randomUUID().toString(),
                )
            } catch (e: Exception) {
                logger.info("Dead connection detected during client count broadcast, unregistering", e)
                iterator.remove()
                deadConnectionRemoved = true
            }
        }

        // If we removed any dead connections, broadcast again with updated count
        if (deadConnectionRemoved) {
            broadcastClientCount()
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
                        img(
                            src = "https://api.qrserver.com/v1/create-qr-code/?data=https://kotlin-htmx.fly.dev/demo/htmx/checkboxes&amp;size=200x200",
                        ) {
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
                            a(href = "https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/") {
                                +"Go here for a lengthy blogpost about the implementation"
                            }
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
                                attributes.hx {
                                    get = "checkboxes/all"
                                    trigger = "sse:update-all"
                                }

                                renderBoxGridHtml()
                            }
                        }

                        // Add JavaScript to count checkboxes and update the counter
                        script {
                            unsafe {
                                raw(
                                    """
                                    // AI generated code for demo purposes 😬
                                        
                                    // Function to count checkboxes and update the counter
                                    function updateCheckboxCounter() {
                                        const checkboxes = document.querySelectorAll('input[type="checkbox"]');
                                        const counter = document.getElementById('loaded-counter');
                                        if (counter) {
                                            counter.textContent = 'Loaded in browser: ' + checkboxes.length;
                                        }
                                    }

                                    // Initial count
                                    updateCheckboxCounter();

                                    // Set up a MutationObserver to detect when new checkboxes are loaded
                                    const observer = new MutationObserver(function(mutations) {
                                        let shouldUpdate = false;

                                        // Check if any of the mutations added checkboxes
                                        mutations.forEach(function(mutation) {
                                            if (mutation.type === 'childList') {
                                                mutation.addedNodes.forEach(function(node) {
                                                    if (node.nodeName === 'INPUT' && node.type === 'checkbox') {
                                                        shouldUpdate = true;
                                                    } else if (node.querySelectorAll) {
                                                        // Check for checkboxes in added subtrees
                                                        const checkboxes = node.querySelectorAll('input[type="checkbox"]');
                                                        if (checkboxes.length > 0) {
                                                            shouldUpdate = true;
                                                        }
                                                    }
                                                });
                                            }
                                        });

                                        // Update the counter if checkboxes were added
                                        if (shouldUpdate) {
                                            updateCheckboxCounter();
                                        }
                                    });

                                    // Start observing the document with the configured parameters
                                    observer.observe(document.body, { childList: true, subtree: true });

                                    // Also update when HTMX completes a request (for infinite scrolling)
                                    document.body.addEventListener('htmx:afterOnLoad', updateCheckboxCounter);
                                    document.body.addEventListener('htmx:afterSettle', updateCheckboxCounter);
                                    """.trimIndent(),
                                )
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
     * Broadcasts the updated client count to all connected clients.
     */
    suspend fun registerOnCheckBoxNotification(session: ServerSSESession) {
        connectedListeners.add(session)
        broadcastClientCount()
    }

    /**
     * Unregisters an SSE session when the connection is closed.
     * Broadcasts the updated client count to all remaining connected clients.
     */
    suspend fun unregisterOnCheckBoxNotification(session: ServerSSESession) {
        this.connectedListeners.remove(session)
        broadcastClientCount()
    }

    /**
     * Returns the current number of connected clients.
     */
    fun getConnectedClientCount(): Int = connectedListeners.size
}
