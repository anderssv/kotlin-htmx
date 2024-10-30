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
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val logger = LoggerFactory.getLogger("no.mikill.kotlin_htmx.Routes")

fun Application.configurePageRoutes(
    mainPage: MainPage,
    selectedPage: SelectedPage,
    applicationRepository: ApplicationRepository
) {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    val validator = Validation.buildDefaultValidatorFactory().validator

    val multiDemoPage = MultiDemoPage()
    val htmxDemoPage = HtmxDemoPage()
    val adminPage = AdminPage()
    val formPage = FormPage()

    routing {
        configureBasicRoutes(mainPage, selectedPage)
        configureDemoRoutes(
            multiDemoPage,
            htmxDemoPage,
            adminPage,
            formPage,
            applicationRepository,
            mapper,
            validator
        )
        configureDataRoutes()
    }
}

private fun Routing.configureBasicRoutes(mainPage: MainPage, selectedPage: SelectedPage) {
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
    get("/{itemName}") {
        selectedPage.renderPage(this)
    }
}

private fun Routing.configureDemoRoutes(
    multiDemoPage: MultiDemoPage,
    htmxDemoPage: HtmxDemoPage,
    adminPage: AdminPage,
    formPage: FormPage,
    applicationRepository: ApplicationRepository,
    mapper: ObjectMapper,
    validator: jakarta.validation.Validator,
) {
    route("/demo") {
        get("/item/{itemId}") {
            val itemId = call.parameters["itemId"]!!.toInt()
            adminPage.renderItemResponse(this, itemId)
        }
        get("/multi") {
            multiDemoPage.renderMultiJsPage(this)
        }
        get("/admin") {
            adminPage.renderAdminPage(this)
        }
        configureHtmxRoutes(htmxDemoPage)
        configureFormRoutes(formPage, applicationRepository, mapper, validator)
    }
}

private fun Route.configureHtmxRoutes(htmxDemoPage: HtmxDemoPage) {
    route("/htmx") {
        get {
            htmxDemoPage.renderPage(this)
        }
        route("/checkboxes") {
            get {
                htmxDemoPage.renderCheckboxesPage(this)
            }
            get("/update") {
                htmxDemoPage.renderBoxGridFragment(this)
            }
            put("{boxNumber}") {
                htmxDemoPage.handleCheckboxToggle(this)
            }
            sse("events") {
                this.send(event = "update-all")
                htmxDemoPage.registerOnCheckBoxNotification(this)
                while (true) {
                    send("ping", "connection")
                    delay(60.seconds)
                }
            }
        }
    }
}

private fun Route.configureFormRoutes(
    formPage: FormPage,
    applicationRepository: ApplicationRepository,
    mapper: ObjectMapper,
    validator: jakarta.validation.Validator
) {
    route("/form") {
        get {
            formPage.renderInputForm(this, null, emptySet())
        }
        post {
            handleFormSubmission(formPage, applicationRepository, mapper, validator)
        }
        get("/{id}/saved") {
            val application = applicationRepository.getApplication(UUID.fromString(call.parameters["id"]!!))!!
            formPage.renderFormSaved(this, application)
        }
    }
}

private suspend fun RoutingContext.handleFormSubmission(
    formPage: FormPage,
    applicationRepository: ApplicationRepository,
    mapper: ObjectMapper,
    validator: jakarta.validation.Validator
) {
    // Since this method takes in repo and a lot of other stuff it probably belongs to a Controller
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
        formPage.renderInputForm(this, updatedApplication, errors)
    } else {
        call.respondRedirect("/demo/form/${updatedApplication.id}/saved")
    }
}

private fun Routing.configureDataRoutes() {
    route("/data") {
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
}