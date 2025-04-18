package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import jakarta.validation.Validation
import jakarta.validation.Validator
import kotlinx.coroutines.delay
import kotlinx.html.*
import kotlinx.io.IOException
import no.mikill.kotlin_htmx.application.ApplicationRepository
import no.mikill.kotlin_htmx.application.Person
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.pages.AdminDemoPage
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.FormDemoPage
import no.mikill.kotlin_htmx.pages.HtmlElements.todoListHtmlContent
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.MainTemplate
import no.mikill.kotlin_htmx.pages.htmx.HtmxCheckboxDemoPage
import no.mikill.kotlin_htmx.pages.htmx.HtmxQuestionsPage
import no.mikill.kotlin_htmx.pages.htmx.HtmxTodolistDemoPage
import no.mikill.kotlin_htmx.selection.pages.SelectMainPage
import no.mikill.kotlin_htmx.selection.pages.SelectedPage
import no.mikill.kotlin_htmx.todo.MultiTodoDemoPage
import no.mikill.kotlin_htmx.todo.todoListItems
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val logger = LoggerFactory.getLogger("no.mikill.kotlin_htmx.Routes")

fun Application.configurePageRoutes(
    lookupClient: LookupClient, applicationRepository: ApplicationRepository
) {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    val validator = Validation.buildDefaultValidatorFactory().validator

    routing {
        get("/robots.txt") {
            call.respond(
                """
                # Allow all crawlers
                User-agent: *
                Allow: /
                """.trimIndent()
            )
        }

        get {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Front page")) {
                mainSectionTemplate {
                    emptyContentWrapper {
                        section {
                            p {
                                +"Demo. Check out my writings at "
                                a(href = "https://blog.f12.no") { +"https://blog.f12.no" }
                                +" or my company page at "
                                a(href = "https://www.mikill.no") { +"https://www.mikill.no" }
                                +"."
                            }
                            p {
                                +"Full source at "
                                a(href = "https://github.com/anderssv/kotlin-htmx/") { +"https://github.com/anderssv/kotlin-htmx/" }
                                +"."
                            }
                        }
                        section {
                            h1 { +"Demos" }
                            +"HTMX and KTor"
                            ul {
                                li {
                                    +"Todolist"
                                    ul {
                                        li { a(href = "/demo/htmx") { +"HTMX component" } }
                                        li { a(href = "/demo/multi") { +"HTML, HTMX, React and Lit component in the same page" } }
                                    }
                                }
                                li { a(href = "/demo/admin") { +"Dynamic content: Admin page for editing data" } }
                                li { a(href = "/select") { +"Wizard:  Flow for selecting a thing. Some HX-Boost and SPA emulation." } }
                                li {
                                    a(href = "/demo/htmx/checkboxes") { +"Checkboxes: Synchronization across browser windows" }
                                    +" - "
                                    a(href = "https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/") { +"Blog entry with description" }
                                }
                                li {
                                    a(href = "/demo/htmx/questions") { +"Questions page: Submit and view questions" }
                                }
                            }
                            +"Just HTML and KTor"
                            ul {
                                li { a(href = "/demo/form") { +"Form handling: Flow handling with validations" } }
                            }
                        }
                    }
                }
            }
        }

        route("/select") {
            val selectMainPage = SelectMainPage(lookupClient)
            val selectedPage = SelectedPage()
            get {
                selectMainPage.renderMainPage(this)
            }
            post("/search") {
                selectMainPage.search(this)
            }
            get("/{itemName}") {
                selectedPage.renderPage(this)
            }
        }
        route("/demo") {
            configureDemoRoutes(
                applicationRepository, mapper, validator
            )
        }
        route("/data") {
            configureDataRoutes()
        }
    }
}

private fun Route.configureDemoRoutes(
    applicationRepository: ApplicationRepository,
    mapper: ObjectMapper,
    validator: Validator,
) {
    val adminDemoPage = AdminDemoPage()
    val multiTodoDemoPage = MultiTodoDemoPage()

    get("/item/{itemId}") {
        val itemId = call.parameters["itemId"]!!.toInt()
        adminDemoPage.renderItemResponse(this, itemId)
    }
    get("/multi") {
        multiTodoDemoPage.renderMultiJsPage(this)
    }
    get("/admin") {
        adminDemoPage.renderAdminPage(this)
    }

    configureHtmxRoutes()
    configureFormRoutes(applicationRepository, mapper, validator)
}

private fun Route.configureHtmxRoutes() {
    val htmxCheckboxDemoPage = HtmxCheckboxDemoPage()
    val htmxTodolistDemoPage = HtmxTodolistDemoPage()
    val htmxQuestionsPage = HtmxQuestionsPage()

    route("/htmx") {
        get {
            htmxTodolistDemoPage.renderHtmxTodoListPage(this)
        }
        route("/questions") {
            get {
                htmxQuestionsPage.renderQuestionsPage(this)
            }
            post("/submit") {
                htmxQuestionsPage.handleQuestionSubmission(this)
            }
        }

        route("/checkboxes") {
            get {
                htmxCheckboxDemoPage.renderCheckboxesPage(this)
            }
            get("/all") {
                htmxCheckboxDemoPage.renderBoxGridFragment(this)
            }
            put("{boxNumber}") {
                htmxCheckboxDemoPage.handleCheckboxToggle(this)
            }
            sse("events") {
                /**
                 * Sending update all event to trigger a full refresh of the page on
                 * reconnects. It's not supported by KTor SSE but is sent by the client
                 * if the connection is lost.
                 *
                 * Ref: https://html.spec.whatwg.org/multipage/server-sent-events.html#last-event-id
                 */
                if (this.call.request.headers["Last-Event-ID"] != null) {
                    this.send(data = "true", event = "update-all", id = UUID.randomUUID().toString())
                    logger.info("SSE Reconnect detected, sending update-all")
                } else {
                    logger.info("SSE First connection")
                }
                htmxCheckboxDemoPage.registerOnCheckBoxNotification(this)

                /**
                 * Looping to keep the connection alive (so the page can publish).
                 * Semi-frequent pings to detect dead connections and unregister.
                 * But it is also handled on the page with the listeners.
                 */
                var alive = true
                while (alive) {
                    try {
                        send("ping", "connection", UUID.randomUUID().toString())
                        delay(10.seconds)
                    } catch (e: IOException) {
                        alive = false
                        htmxCheckboxDemoPage.unregisterOnCheckBoxNotification(this)
                        logger.debug("Detected dead connection, unregistering", e)
                    }
                }
            }
        }
    }
}

private fun Route.configureFormRoutes(
    applicationRepository: ApplicationRepository, mapper: ObjectMapper, validator: Validator
) {
    val formDemoPage = FormDemoPage()
    route("/form") {
        get {
            formDemoPage.renderInputForm(this, null, emptySet())
        }
        post {
            handleFormSubmission(formDemoPage, applicationRepository, mapper, validator)
        }
        get("/{id}/saved") {
            val application = applicationRepository.getApplication(UUID.fromString(call.parameters["id"]!!))!!
            formDemoPage.renderFormSaved(this, application)
        }
    }
}

private suspend fun RoutingContext.handleFormSubmission(
    formDemoPage: FormDemoPage, applicationRepository: ApplicationRepository, mapper: ObjectMapper, validator: Validator
) {
    // Since this method takes in repo and a lot of other stuff, it probably belongs to a Controller
    val form = call.receiveParameters()
    logger.info("Received form data: $form")

    val application = no.mikill.kotlin_htmx.application.Application(
        UUID.randomUUID(), Person("", ""), ""
    )
    val updatedApplication: no.mikill.kotlin_htmx.application.Application =
        mapper.readerForUpdating(application).readValue(form["__formjson"]!!)
    applicationRepository.addApplication(updatedApplication)

    val errors = validator.validate(updatedApplication)
    if (errors.isNotEmpty()) {
        formDemoPage.renderInputForm(this, updatedApplication, errors)
    } else {
        call.respondRedirect("/demo/form/${updatedApplication.id}/saved")
    }
}


private fun Route.configureDataRoutes() {
    get("/todolist.json") {
        call.respond(todoListItems)
    }
    get("/todolist.html") {
        val delaySeconds = call.parameters["delay"]?.toInt() ?: 1
        delay(delaySeconds.seconds)
        call.respondHtmlFragment {
            todoListHtmlContent("htmx")
        }
    }
}
