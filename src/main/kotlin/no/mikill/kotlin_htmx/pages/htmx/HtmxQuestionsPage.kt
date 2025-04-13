package no.mikill.kotlin_htmx.pages.htmx

import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.MainTemplate
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class HtmxQuestionsPage {
    private val logger = LoggerFactory.getLogger(HtmxQuestionsPage::class.java)

    // Maximum size of 1000 to prevent DDoS attacks
    private val MAX_QUESTIONS = 1000

    // Using LinkedHashMap with a maximum size specified at creation time
    // Wrapped in Collections.synchronizedMap for thread safety
    private val questions =
        Collections.synchronizedMap(object : LinkedHashMap<UUID, Question>(MAX_QUESTIONS, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, Question>?): Boolean {
                return size > MAX_QUESTIONS
            }
        })

    data class Question(
        val id: UUID = UUID.randomUUID(),
        val text: String,
        val timestamp: ZonedDateTime = ZonedDateTime.now()
    )

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
                                    attributes["hx-on::after-request"] = "if(event.detail.successful) this.reset()"

                                    div {
                                        style = "display: flex; gap: 10px;"
                                        textArea {
                                            id = "question-input"
                                            name = "question"
                                            placeholder = "Type your question here..."
                                            required = true
                                            style = "flex-grow: 1; padding: 8px; min-height: 60px; resize: vertical; overflow-wrap: break-word; word-wrap: break-word;"
                                        }
                                        button(type = ButtonType.submit) {
                                            id = "submit-button"
                                            style = "padding: 8px 16px;"
                                            attributes["hx-post"] = "questions/submit"
                                            attributes["hx-target"] = "#questions-list"
                                            attributes["hx-swap"] = "innerHTML"
                                            attributes["hx-indicator"] = ".htmx-indicator"
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
