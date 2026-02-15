@file:OptIn(ExperimentalKtorApi::class)

package no.mikill.kotlin_htmx.pages.htmx

import io.ktor.htmx.HxSwap
import io.ktor.htmx.html.hx
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.RoutingContext
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.style
import kotlinx.html.textArea
import kotlinx.html.ul
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.MainTemplate
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.UUID

/**
 * Demo page that showcases a simple question submission and display system using HTMX.
 *
 * This implementation demonstrates how to:
 * - Create a form with HTMX attributes for submission without page reload
 * - Handle form submission and reset the form after successful submission
 * - Display dynamic content updates
 * - Implement a thread-safe cache with automatic size management
 */
class HtmxQuestionsPage {
    private val logger = LoggerFactory.getLogger(HtmxQuestionsPage::class.java)

    // Maximum size of 1000 to prevent DDoS attacks
    private val MAX_QUESTIONS = 1000

    /**
     * Thread-safe question storage with automatic eviction of oldest entries.
     *
     * Uses:
     * - LinkedHashMap with access-order to track usage (true in the constructor)
     * - Collections.synchronizedMap for thread safety
     * - Custom removeEldestEntry implementation to limit the maximum size
     *
     * This approach provides an efficient LRU (Least Recently Used) cache
     * that automatically removes the oldest questions when the maximum size is reached.
     */
    private val questions =
        Collections.synchronizedMap(
            object : LinkedHashMap<UUID, Question>(MAX_QUESTIONS, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, Question>?): Boolean = size > MAX_QUESTIONS
            },
        )

    /**
     * Data class representing a question submitted by a user.
     *
     * @property id Unique identifier for the question, defaults to a random UUID
     * @property text The content of the question
     * @property timestamp When the question was submitted, defaults to current time
     */
    data class Question(
        val id: UUID = UUID.randomUUID(),
        val text: String,
        val timestamp: ZonedDateTime = ZonedDateTime.now(),
    )

    /**
     * Renders the complete questions page with a submission form and list of existing questions.
     *
     * This method sets up:
     * - The page header with title and description
     * - A question submission form with HTMX attributes for dynamic submission
     * - A list of existing questions
     *
     * @param context The routing context for the request
     */
    suspend fun renderQuestionsPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "HTMX Questions Page")) {
                headerContent {
                    section {
                        h1 { +"Questions Page" }
                        p {
                            +"Submit your questions and see what others have asked."
                        }
                    }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        div {
                            style = "max-width: 40em;"

                            // Question submission form
                            div {
                                style = "margin-bottom: 20px;"
                                h2 { +"Ask a Question" }
                                form {
                                    id = "question-form"
                                    // HTMX event handler to reset the form after successful submission
                                    // This allows users to submit multiple questions without manually clearing the form
                                    // NOTE: Using manual attribute due to Ktor bug - on() generates hx-on:after-request
                                    // but HTMX 2.0 requires hx-on::after-request (double colon) for HTMX events
                                    attributes["hx-on::after-request"] = "if(event.detail.successful) this.reset()"

                                    div {
                                        style = "display: flex; gap: 10px;"
                                        textArea {
                                            id = "question-input"
                                            name = "question"
                                            placeholder = "Type your question here..."
                                            required = true
                                            style =
                                                "flex-grow: 1; padding: 8px; min-height: 60px; resize: vertical; overflow-wrap: break-word; word-wrap: break-word;"
                                        }
                                        button(type = ButtonType.submit) {
                                            id = "submit-button"
                                            style = "padding: 8px 16px;"
                                            // HTMX attributes for form submission using DSL
                                            attributes.hx {
                                                post = "questions/submit"
                                                target = "#questions-list"
                                                swap = HxSwap.innerHtml
                                                indicator = ".htmx-indicator"
                                            }
                                            +"Submit"
                                        }
                                    }

                                    // Loading indicator
                                    div(classes = "htmx-indicator") {
                                        style = "display: none; margin-top: 10px;"
                                        +"Submitting question..."
                                    }
                                }
                            }

                            // Questions list
                            div {
                                h2 { +"Questions" }
                                div {
                                    id = "questions-list"
                                    renderQuestionsList()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles the submission of a new question from the form.
     *
     * This method:
     * 1. Extracts the question text from the form parameters
     * 2. Validates that the question is not empty
     * 3. Creates a new Question object with a unique ID
     * 4. Adds the question to the thread-safe cache
     * 5. Responds with an updated HTML fragment of the questions list
     *
     * @param context The routing context for the request
     */
    suspend fun handleQuestionSubmission(context: RoutingContext) {
        with(context) {
            val formParameters = call.receiveParameters()
            logger.info("Received form parameters: $formParameters")
            val questionText = formParameters["question"]?.trim()
            logger.info("Extracted question text: '$questionText'")

            if (!questionText.isNullOrBlank()) {
                val questionId = UUID.randomUUID()
                val newQuestion = Question(id = questionId, text = questionText)

                // Add the new question
                // The LinkedHashMap will automatically remove the oldest entry if the maximum size is reached
                questions[questionId] = newQuestion
                logger.info("New question submitted: $questionText with ID: $questionId")
                logger.info("Current questions map size: ${questions.size}")
            } else {
                logger.warn("Received blank or null question text")
            }

            call.respondHtmlFragment {
                renderQuestionsList()
            }
        }
    }

    /**
     * Renders the list of submitted questions as HTML.
     *
     * This method:
     * - Shows a message if no questions have been submitted yet
     * - Otherwise, displays questions in a styled unordered list
     * - Sorts questions by timestamp (newest first)
     * - Formats each question with text and submission time
     *
     * The HTML generated by this method is returned as a fragment
     * when a new question is submitted, allowing for dynamic updates
     * without a full page reload.
     */
    private fun FlowContent.renderQuestionsList() {
        if (questions.isEmpty()) {
            p { +"No questions have been asked yet. Be the first to ask a question!" }
        } else {
            ul {
                style = "list-style-type: none; padding: 0;"
                // Sort by timestamp (newest first) and convert to list for display
                questions.values
                    .sortedByDescending { it.timestamp }
                    .forEach { question ->
                        li {
                            style = "border: 1px solid #ddd; padding: 10px; margin-bottom: 10px; border-radius: 5px;"
                            div {
                                style = "font-weight: bold; word-wrap: break-word; overflow-wrap: break-word;"
                                +question.text
                            }
                            div {
                                style = "font-size: 0.8em; color: #666; margin-top: 5px;"
                                +"Asked on ${question.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"
                            }
                        }
                    }
            }
        }
    }
}
