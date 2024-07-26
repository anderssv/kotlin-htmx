import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.mikill.kotlin_htmx.pages.DemoPage
import no.mikill.kotlin_htmx.pages.MainPage
import no.mikill.kotlin_htmx.pages.SelectedPage

fun Application.configurePageRoutes(
    mainPage: MainPage,
    selectedPage: SelectedPage
) {

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

        get("/demo") {
            DemoPage().renderPage(this)
        }
    }
}