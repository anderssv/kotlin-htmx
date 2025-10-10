@file:OptIn(ExperimentalKtorApi::class)

package no.mikill.kotlin_htmx

import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.htmx.hx
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.sse
import io.ktor.utils.io.ExperimentalKtorApi
import jakarta.validation.Validation
import kotlinx.coroutines.delay
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.ul
import kotlinx.io.IOException
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.pages.AdminDemoPage
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlElements.todoListHtmlContent
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.MainTemplate
import no.mikill.kotlin_htmx.pages.htmx.HtmxCheckboxDemoPage
import no.mikill.kotlin_htmx.pages.htmx.HtmxQuestionsPage
import no.mikill.kotlin_htmx.pages.htmx.HtmxTodolistDemoPage
import no.mikill.kotlin_htmx.registration.PersonRepository
import no.mikill.kotlin_htmx.registration.configurePersonRegistrationRoutes
import no.mikill.kotlin_htmx.selection.pages.SelectMainPage
import no.mikill.kotlin_htmx.selection.pages.SelectedPage
import no.mikill.kotlin_htmx.todo.HtmlTodoDemoPage
import no.mikill.kotlin_htmx.todo.MultiTodoDemoPage
import no.mikill.kotlin_htmx.todo.todoListItems
import no.mikill.kotlin_htmx.validation.ValidationService
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private val routesLogger = LoggerFactory.getLogger("no.mikill.kotlin_htmx.Routes")

/**
 * Configures all page routes for the application.
 * This is the main entry point for setting up the application's routing structure.
 *
 * @param lookupClient Client for external lookup services
 */
fun Application.configurePageRoutes(
    lookupClient: LookupClient,
    numberOfCheckboxes: Int,
) {
    val validatorFactory = Validation.buildDefaultValidatorFactory()
    val validationService = ValidationService(validatorFactory.validator)

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
            configureDemoRoutes(numberOfCheckboxes)
        }

        route("/data") {
            configureDataRoutes()
        }
    }

    // Person registration routes
    val personRepository = PersonRepository()
    configurePersonRegistrationRoutes(personRepository, validationService)
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
            """.trimIndent(),
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
                                        a(
                                            href = "https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/",
                                        ) { +"Blog entry with description" }
                                    }
                                    li {
                                        a(href = "/demo/htmx/questions") { +"Questions page: Submit and view questions" }
                                    }
                                }
                            }
                        }
                        +"Just HTML and KTor"
                        ul {
                            li { a(href = "/person/register") { +"Person Registration: Multi-step form with type-safe binding and validation" } }
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

    // Use HTMX-specific routing for search
    hx.post("/search") {
        selectMainPage.search(this)
    }

    get("/{itemName}") {
        selectedPage.renderPage(this)
    }
}

/**
 * Configures all demo routes including HTML and HTMX demos
 */
private fun Route.configureDemoRoutes(numberOfCheckboxes: Int) {
    val adminDemoPage = AdminDemoPage()
    val multiTodoDemoPage = MultiTodoDemoPage()
    val htmlTodoDemoPage = HtmlTodoDemoPage()

    // Basic HTML demo routes
    get {
        htmlTodoDemoPage.renderHtmlPage(this)
    }

    get("/multi") {
        // todoListItems would normally be fetched from a database
        multiTodoDemoPage.renderMultiJsPage(this, todoListItems)
    }

    get("/admin") {
        adminDemoPage.renderAdminPage(this)
    }

    // Use HTMX-specific routing for admin item updates
    hx.get("/item/{itemId}") {
        call.response.header(HttpHeaders.CacheControl, CacheControl.MaxAge(maxAgeSeconds = 30).toString())
        val itemId = call.parameters["itemId"]!!.toInt()
        adminDemoPage.renderItemResponse(this, itemId)
    }

    // Configure specialized demo routes
    configureHtmxRoutes(numberOfCheckboxes)
}

/**
 * Configures HTMX-specific demo routes including todolist, questions, and checkboxes
 */
private fun Route.configureHtmxRoutes(numberOfCheckboxes: Int) {
    val htmxCheckboxDemoPage = HtmxCheckboxDemoPage(numberOfCheckboxes)
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
            // Use HTMX-specific routing for form submission
            hx.post("/submit") {
                htmxQuestionsPage.handleQuestionSubmission(this)
            }
        }

        // Checkboxes demo with SSE for real-time updates
        route("/checkboxes") {
            get {
                htmxCheckboxDemoPage.renderCheckboxesPage(this)
            }

            // Use HTMX-specific routing for dynamic content updates
            hx.get("/all") {
                htmxCheckboxDemoPage.renderBoxGridFragment(this)
            }

            hx.get("/batch/{batchNumber}") {
                htmxCheckboxDemoPage.renderBoxBatch(this)
            }

            hx.put("{boxNumber}") {
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
 * Configures routes for data endpoints that provide JSON and HTML fragments
 */
private fun Route.configureDataRoutes() {
    // JSON data endpoint
    get("/todolist.json") {
        call.respond(todoListItems)
    }

    // HTML fragment endpoint with simulated delay - use HTMX-specific routing
    hx.get("/todolist.html") {
        val delaySeconds = call.parameters["delay"]?.toInt() ?: 1
        delay(delaySeconds.seconds)
        call.respondHtmlFragment {
            // todoListItems would normally be fetched from a database
            todoListHtmlContent("htmx", todoListItems)
        }
    }
}
