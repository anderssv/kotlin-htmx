package no.mikill.kotlin_htmx

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.sse
import jakarta.validation.Validation
import kotlinx.coroutines.delay
import no.mikill.kotlin_htmx.application.ApplicationRepository
import no.mikill.kotlin_htmx.application.Person
import no.mikill.kotlin_htmx.pages.*
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.todoListHtmlContent
import no.mikill.kotlin_htmx.pages.HtmlElements.respondHtmlFragment
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

    val multiDemoPage = MultiDemoPage()
    val htmxDemoPage = HtmxDemoPage()
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
            get("/item/{itemId}") {
                val itemId = call.parameters["itemId"]!!.toInt()
                adminPage.renderItemResponse(this, itemId)
            }

            get("/multi") {
                multiDemoPage.renderMultiJsPage(this)
            }

            route("/htmx") {
                get {
                    htmxDemoPage.renderPage(this)
                }

                route("/checkboxes") {
                    get {
                        htmxDemoPage.renderCheckboxesPage(this)
                    }
                    get("/update") {
                        htmxDemoPage.boxGridFragment(this)
                    }
                    put("{boxNumber}") {
                        htmxDemoPage.toggle(this)
                    }
                    sse("events") {
                        this.send(
                            "",
                            "update-all"
                        ) // Fetches on reconnect. Shouldn't really trigger on first connect, but don't know a way to detect re-connects
                        htmxDemoPage.onCheckboxUpdate { boxNumber, checkedState ->
                            sse@ this.send(HtmlElements.partialHtml {
                                checkbox(boxNumber, checkedState)
                            }, "update-$boxNumber")
                        }
                        // It seems it terminates the connection without this.
                        while (true) {
                            // This will fail once the connection is closes, should probably have some handling
                            send("ping", "connection")
                            delay(60.seconds)
                        }
                    }
                }
            }

            get("/admin") {
                adminPage.renderAdminPage(this)
            }

            route("/form") {
                get {
                    formPage.renderInputForm(this, null, emptySet())
                }
                post {
                    // This logic should probably be in a type of controller to make the route setup clearer and isolated
                    val form = call.receiveParameters()
                    logger.info("Received form data: $form")

                    /*
                     * Creating a new application here with some bogus values.
                     *
                     * It is a common problem how to handle partially filled forms
                     * and invalid data, but we won't demonstrate that here.
                     *
                     * Things like Sum Types (https://github.com/anderssv/the-example/blob/main/doc/sum-types.md)
                     * could probably be used here and should be explored.
                     *
                     * The other option is, of course, to set everything to be nullable,
                     * but it seems like a less than ideal option.
                     */
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

                get("/{id}/saved") {
                    val application = applicationRepository.getApplication(UUID.fromString(call.parameters["id"]!!))!!
                    formPage.renderFormSaved(this, application)
                }
            }
        }

        route("/data") {
            get("/todolist.json") {
                // This is just a JSON string now, but would usually be generated by Jackson from the domain and DB.
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
                val delaySeconds = call.parameters["delay"]?.toInt() ?: 1
                delay(delaySeconds.seconds)
                call.respondHtmlFragment {
                    todoListHtmlContent("htmx")
                }
            }
        }
    }
}