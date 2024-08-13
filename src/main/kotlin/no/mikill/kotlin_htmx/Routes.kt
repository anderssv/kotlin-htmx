package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jakarta.validation.Validation
import kotlinx.coroutines.delay
import no.mikill.kotlin_htmx.application.ApplicationRepository
import no.mikill.kotlin_htmx.application.Person
import no.mikill.kotlin_htmx.pages.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds


fun Application.configurePageRoutes(
    mainPage: MainPage,
    selectedPage: SelectedPage,
    applicationRepository: ApplicationRepository
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    val validator = Validation.buildDefaultValidatorFactory().validator

    val demoPage = DemoPage()
    val adminPage = AdminPage()
    val formPage = FormPage()

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
        get("/") {
            mainPage.renderMainPage(this)
        }

        post("/search") {
            mainPage.search(this)
        }

        route("/{itemName}") {
            get {
                selectedPage.renderPage(this)
            }
        }

        route("/demo") {
            get("/multi") {
                demoPage.renderMultiJsPage(this)
            }
            get("/form") {
                formPage.renderInputForm(this, null, emptySet())
            }
            post("/form") {
                val form = call.receiveParameters()
                logger.info("Received form data: $form")

                val application = no.mikill.kotlin_htmx.application.Application(
                    UUID.randomUUID(),
                    Person("", ""),
                    ""
                ) // This would typically be fetched from a DB based on a ID, but don't have that right now
                val updatedApplication: no.mikill.kotlin_htmx.application.Application =
                    mapper.readerForUpdating(application).readValue(form["_formjson"]!!)
                applicationRepository.addApplication(updatedApplication) // And then of course this would be an update, not an add

                val errors = validator.validate(updatedApplication)
                if (errors.isNotEmpty()) { // Back to same page with errors
                    formPage.renderInputForm(this, updatedApplication, errors)
                } else {
                    call.respondRedirect("/demo/form/${updatedApplication.id}/saved")
                }
            }
            get("/form/{id}/saved") {
                val application = applicationRepository.getApplication(UUID.fromString(call.parameters["id"]!!))!!
                formPage.renderFormSaved(this, application)
            }

            get("/admin") {
                adminPage.renderAdminPage(this)
            }

            get("/item/{itemId}") {
                val itemId = call.parameters["itemId"]!!.toInt()
                adminPage.renderItemResponse(this, itemId)
            }
        }

        route("/data") {
            get("/todolist.json") {
                call.respondText(
                    """
                        [
                          {
                            "id": 1,
                            "title": "Buy milk",
                            "completed": false
                          },
                          {
                            "id": 2,
                            "title": "Buy bread",
                            "completed": false
                          },
                          {
                            "id": 3,
                            "title": "Buy eggs",
                            "completed": false
                          },
                          {
                            "id": 4,
                            "title": "Buy butter",
                            "completed": false
                          }
                        ]
                    """.trimIndent(),
                    ContentType.Application.Json
                )
            }
            get("/todolist.html") {
                delay(1.seconds)
                call.respondText(
                    """
                    <h1>Todo List</h1>
                    <ul id="todo-list">
                        <li>Buy milk</li>
                        <li>Buy bread</li>
                        <li>Buy eggs</li>
                        <li>Buy butter</li>
                    </ul>
                    <p>It is now <span id="htmx-date"></span></p>
                    <script>
                        document.getElementById('htmx-date').innerHTML = new Date().toLocaleString();
                    </script>
                """.trimIndent()
                )
            }
        }
    }
}