import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import no.mikill.kotlin_htmx.pages.DemoPage
import no.mikill.kotlin_htmx.pages.MainPage
import no.mikill.kotlin_htmx.pages.SelectedPage
import kotlin.time.Duration.Companion.seconds

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
                delay(5.seconds)
                call.respondText("""
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
                """.trimIndent())
            }
        }
    }
}