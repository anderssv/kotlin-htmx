package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
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
import no.mikill.kotlin_htmx.todo.HtmlTodoDemoPage
import no.mikill.kotlin_htmx.todo.MultiTodoDemoPage
import no.mikill.kotlin_htmx.todo.todoListItems
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val routesLogger = LoggerFactory.getLogger("no.mikill.kotlin_htmx.Routes")

/**
 * Configures all page routes for the application.
 * This is the main entry point for setting up the application's routing structure.
 *
 * @param lookupClient Client for external lookup services
 * @param applicationRepository Repository for application data
 */
fun Application.configurePageRoutes(
    lookupClient: LookupClient, applicationRepository: ApplicationRepository
) {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    val validator = Validation.buildDefaultValidatorFactory().validator

    routing {
        // Standard routes
        configureStaticRoutes()

        // Main page route
        configureMainPageRoute()

        // Feature-specific routes
        route("/select") {
            configureSelectionRoutes(lookupClient)
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

/**
 * Configures static routes like robots.txt
 */
private fun Route.configureStaticRoutes() {
    get("/robots.txt") {
        call.respond(
            """
            # Allow all crawlers
            User-agent: *
            Allow: /
            """.trimIndent()
        )
    }
}

/**
 * Configures the main landing page with links to all demos
 */
private fun Route.configureMainPageRoute() {
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
                                    li { a(href = "/demo") { +"Plain HTML component" } }
                                    li { a(href = "/demo/htmx") { +"HTMX component" } }
                                    li { a(href = "/demo/multi") { +"HTML, HTMX, React and Lit component in the same page" } }
                                }
                            }
                            li {
                                +"Dynamic content"
                                ul {
                                    li { a(href = "/demo/admin") { +"Admin page for editing data" } }
                                    li { a(href = "/select") { +"Wizard Flow for selecting a thing. Some HX-Boost and SPA emulation." } }
                                    li {
                                        a(href = "/demo/htmx/checkboxes") { +"Checkboxes: Synchronization across browser windows" }
                                        +" - "
                                        a(href = "https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/") { +"Blog entry with description" }
                                    }
                                    li {
                                        a(href = "/demo/htmx/questions") { +"Questions page: Submit and view questions" }
                                    }
                                }
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
}

/**
 * Configures routes for the selection demo
 * 
 * @param lookupClient Client for external lookup services
 */
private fun Route.configureSelectionRoutes(lookupClient: LookupClient) {
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

/**
 * Configures all demo routes including HTML, HTMX, and form demos
 * 
 * @param applicationRepository Repository for application data
 * @param mapper JSON object mapper for serialization/deserialization
 * @param validator Bean validator for form validation
 */
private fun Route.configureDemoRoutes(
    applicationRepository: ApplicationRepository,
    mapper: ObjectMapper,
    validator: Validator,
) {
    val adminDemoPage = AdminDemoPage()
    val multiTodoDemoPage = MultiTodoDemoPage()
    val htmlTodoDemoPage = HtmlTodoDemoPage()

    // Basic HTML demo routes
    get {
        htmlTodoDemoPage.renderHtmlPage(this)
    }

    get("/multi") {
        multiTodoDemoPage.renderMultiJsPage(this)
    }

    get("/admin") {
        adminDemoPage.renderAdminPage(this)
    }

    get("/item/{itemId}") {
        call.response.header(HttpHeaders.CacheControl, CacheControl.MaxAge(maxAgeSeconds = 30).toString())
        val itemId = call.parameters["itemId"]!!.toInt()
        adminDemoPage.renderItemResponse(this, itemId)
    }

    // Configure specialized demo routes
    configureHtmxRoutes()
    configureFormRoutes(applicationRepository, mapper, validator)
}

/**
 * Configures HTMX-specific demo routes including todolist, questions, and checkboxes
 */
private fun Route.configureHtmxRoutes() {
    val htmxCheckboxDemoPage = HtmxCheckboxDemoPage()
    val htmxTodolistDemoPage = HtmxTodolistDemoPage()
    val htmxQuestionsPage = HtmxQuestionsPage()

    route("/htmx") {
        get {
            htmxTodolistDemoPage.renderHtmxTodoListPage(this)
        }

        // Questions demo
        route("/questions") {
            get {
                htmxQuestionsPage.renderQuestionsPage(this)
            }
            post("/submit") {
                htmxQuestionsPage.handleQuestionSubmission(this)
            }
        }

        // Checkboxes demo with SSE for real-time updates
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

            // Server-Sent Events endpoint for real-time updates
            sse("events") {
                handleSseConnection(htmxCheckboxDemoPage)
            }
        }
    }
}

/**
 * Handles Server-Sent Events (SSE) connection for the checkbox demo
 * 
 * @param htmxCheckboxDemoPage The checkbox demo page handler
 */
private suspend fun ServerSSESession.handleSseConnection(htmxCheckboxDemoPage: HtmxCheckboxDemoPage) {
    // Handle reconnection by checking for Last-Event-ID header
    if (this.call.request.headers["Last-Event-ID"] != null) {
        this.send(data = "true", event = "update-all", id = UUID.randomUUID().toString())
        routesLogger.info("SSE Reconnect detected, sending update-all")
    } else {
        routesLogger.info("SSE First connection")
    }

    // Register this connection for checkbox updates
    htmxCheckboxDemoPage.registerOnCheckBoxNotification(this)

    // Keep the connection alive with periodic pings
    // This also helps detect and clean up dead connections
    var alive = true
    while (alive) {
        try {
            send("ping", "connection", UUID.randomUUID().toString())
            delay(10.seconds)
        } catch (e: IOException) {
            alive = false
            htmxCheckboxDemoPage.unregisterOnCheckBoxNotification(this)
            routesLogger.debug("Detected dead connection, unregistering", e)
        }
    }
}

/**
 * Configures form handling demo routes
 * 
 * @param applicationRepository Repository for application data
 * @param mapper JSON object mapper for serialization/deserialization
 * @param validator Bean validator for form validation
 */
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

/**
 * Handles form submission, validation, and persistence
 * 
 * Note: In a larger application, this logic would typically be moved to a dedicated controller class
 * to better separate concerns and improve testability.
 * 
 * @param formDemoPage The form demo page handler
 * @param applicationRepository Repository for application data
 * @param mapper JSON object mapper for serialization/deserialization
 * @param validator Bean validator for form validation
 */
private suspend fun RoutingContext.handleFormSubmission(
    formDemoPage: FormDemoPage, 
    applicationRepository: ApplicationRepository, 
    mapper: ObjectMapper, 
    validator: Validator
) {
    val form = call.receiveParameters()
    routesLogger.info("Received form data: $form")

    // Create a new application instance with default values
    val application = no.mikill.kotlin_htmx.application.Application(
        UUID.randomUUID(), Person("", ""), ""
    )

    // Update the application with form data
    val updatedApplication: no.mikill.kotlin_htmx.application.Application =
        mapper.readerForUpdating(application).readValue(form["__formjson"]!!)

    // Save the application
    applicationRepository.addApplication(updatedApplication)

    // Validate and handle errors or redirect to success page
    val errors = validator.validate(updatedApplication)
    if (errors.isNotEmpty()) {
        formDemoPage.renderInputForm(this, updatedApplication, errors)
    } else {
        call.respondRedirect("/demo/form/${updatedApplication.id}/saved")
    }
}

/**
 * Configures routes for data endpoints that provide JSON and HTML fragments
 */
private fun Route.configureDataRoutes() {
    // JSON data endpoint
    get("/todolist.json") {
        call.respond(todoListItems)
    }

    // HTML fragment endpoint with simulated delay
    get("/todolist.html") {
        val delaySeconds = call.parameters["delay"]?.toInt() ?: 1
        delay(delaySeconds.seconds)
        call.respondHtmlFragment {
            todoListHtmlContent("htmx")
        }
    }
}
