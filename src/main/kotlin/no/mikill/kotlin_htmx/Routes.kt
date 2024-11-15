package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.*
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
import no.mikill.kotlin_htmx.pages.*
import no.mikill.kotlin_htmx.pages.HtmlElements.todoListHtmlContent
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.htmx.HtmxCheckboxDemoPage
import no.mikill.kotlin_htmx.pages.htmx.HtmxTodolistDemoPage
import no.mikill.kotlin_htmx.selection.pages.SelectMainPage
import no.mikill.kotlin_htmx.selection.pages.SelectedPage
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val logger = LoggerFactory.getLogger("no.mikill.kotlin_htmx.Routes")

fun Application.configurePageRoutes(
    lookupClient: LookupClient,
    applicationRepository: ApplicationRepository
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
                            p { +"Demos:" }
                            ul {
                                li {
                                    +"HTMX and KTor"
                                    ul {
                                        li {
                                            a(href = "/demo/htmx/checkboxes") { +"Checkboxes with synchronization across browser windows" }
                                            +" - "
                                            a(href = "https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/") { +"Blog entry with description" }
                                        }
                                        li { a(href = "/select") { +"Select a thing wizard. Some HX-Boost and SPA emulation." } }

                                        li { a(href = "/demo/admin") { +"Simple admin page operations" } }
                                        li { a(href = "/demo/multi") { +"Web component together with React and Lit in the same page" } }
                                    }
                                }
                                li {
                                    +"Just HTML and KTor"
                                    ul {
                                        li { a(href = "/demo/form") { +"Just HTML form - Form flow handling with validations" } }
                                    }
                                }
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
                applicationRepository,
                mapper,
                validator
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
    val multiDemoPage = MultiDemoPage()

    get("/item/{itemId}") {
        val itemId = call.parameters["itemId"]!!.toInt()
        adminDemoPage.renderItemResponse(this, itemId)
    }
    get("/multi") {
        multiDemoPage.renderMultiJsPage(this)
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

    route("/htmx") {
        get {
            htmxTodolistDemoPage.renderHtmxTodoListPage(this)
        }
        route("/checkboxes") {
            get {
                htmxCheckboxDemoPage.renderCheckboxesPage(this)
            }
            get("/update") {
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
    applicationRepository: ApplicationRepository,
    mapper: ObjectMapper,
    validator: Validator
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
    formDemoPage: FormDemoPage,
    applicationRepository: ApplicationRepository,
    mapper: ObjectMapper,
    validator: Validator
) {
    // Since this method takes in repo and a lot of other stuff, it probably belongs to a Controller
    val form = call.receiveParameters()
    logger.info("Received form data: $form")

    val application = no.mikill.kotlin_htmx.application.Application(
        UUID.randomUUID(),
        Person("", ""),
        ""
    )
    val updatedApplication: no.mikill.kotlin_htmx.application.Application =
        mapper.readerForUpdating(application).readValue(form["_formjson"]!!)
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
        call.respondText(
            """
                [
                  {"id": 1, "title": "Buy milk", "completed": false},
                  {"id": 2, "title": "Buy bread", "completed": false},
                  {"id": 3, "title": "Buy eggs", "completed": false},
                  {"id": 4, "title": "Buy butter", "completed": false}
                ]
                """.trimIndent(),
            ContentType.Application.Json
        )
    }
    get("/todolist.html") {
        val delaySeconds = call.parameters["delay"]?.toInt() ?: 1
        delay(delaySeconds.seconds)
        call.respondHtmlFragment {
            todoListHtmlContent("htmx")
        }
    }
}