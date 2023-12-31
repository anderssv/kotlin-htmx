package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.css.div
import kotlinx.html.*
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.integration.LookupResult
import no.mikill.kotlin_htmx.items
import no.mikill.kotlin_htmx.pages.HtmlElements.gridElement
import no.mikill.kotlin_htmx.pages.HtmlElements.htmlFragment
import no.mikill.kotlin_htmx.pages.HtmlElements.respondFullPage
import no.mikill.kotlin_htmx.pages.HtmlElements.selectBox
import org.slf4j.LoggerFactory
import kotlin.collections.set


class MainPage(
    private val lookupClient: LookupClient,
) {
    private val logger = LoggerFactory.getLogger(MainPage::class.java)

    private data class Search(val lookupValue: String)

    suspend fun search(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            val search: Search = call.receive()
            val result = when (val lookupResult = lookupClient.lookup(search.lookupValue)) {
                is LookupResult.Success -> {
                    val item = items.single { it.name == lookupResult.response }

                    call.response.header("HX-Redirect", item.name)
                    // Probably won't show but adding content anyway
                    htmlFragment {
                        div {
                            +"Found it! ${item.name}"
                        }
                    }
                }

                is LookupResult.NotFound -> htmlFragment {
                    div(classes = "text-red-800") {
                        p { +"Could not locate item with '${search.lookupValue}'." }
                        a(href = "/") { +"Try again" }
                    }
                }

                is LookupResult.InvalidInput -> htmlFragment {
                    div(classes = "text-red-800") {
                        p { +lookupResult.message }
                        a(href = "/") { +"Try again" }
                    }
                }

                is LookupResult.Failure -> {
                    logger.error("Lookup failed. Reason: ${lookupResult.reason}")
                    htmlFragment {
                        div(classes = "text-red-800") {
                            p { +"We're sorry. Something went wrong. We'll fix it ASAP." }
                            a(href = "/") { +"Try again" }
                        }
                    }
                }

            }
            call.respond(result)
        }
    }

    suspend fun renderMainPage(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            respondFullPage(
                // Using a pop-over here so a bit special handling. Might consider using as default.
                localStyle = """
                        .htmx-indicator {
                            visibility: hidden;
                        }
                        .htmx-request .htmx-indicator{
                            visibility: visible;
                        }
                        .htmx-request.htmx-indicator{
                            visibility: visible;
                        }                        
                    """.trimIndent()
            ) {
                section(classes = "h-48 w-full text-center no.mikill.`kotlin-htmx`.getItems-center justify-center border drop-shadow-lg border-gray-400 rounded-lg bg-white p-2") {
                    div(classes = "htmx-indicator absolute top-0 left-0 w-full h-full flex no.mikill.`kotlin-htmx`.getItems-center justify-center z-10 bg-white rounded-lg") {
                        id = "formLoading"
                        +"Searching..."
                    }
                    div(classes = "h-full flex no.mikill.`kotlin-htmx`.getItems-center justify-center") {
                        form {
                            attributes["hx-post"] = "/search"
                            attributes["hx-swap"] = "outerHTML"
                            attributes["hx-indicator"] = "#formLoading"

                            div(classes = "flex justify-center") {
                                input(classes = "mr-1 p-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent") {
                                    type = InputType.text
                                    name = "lookupValue"
                                    attributes["aria-label"] = "Value"
                                    required = true
                                }
                                button(classes = "px-5 py-2 text-white bg-blue-800 rounded-md hover:bg-blue-600 focus:outline-none focus:bg-blue-600") {
                                    attributes["aria-label"] = "Check"
                                    +"Check"
                                }
                            }
                            div { +"Input one of the item names below, anything else will generate an not found error" }
                            div(classes = "m-4") {
                                +"The source and description of the code for this site: "
                                a("https://github.com/anderssv/kotlin-htmx/blob/main/Readme.md") {
                                    +"https://github.com/anderssv/kotlin-htmx/blob/main/Readme.md"
                                }
                            }
                        }
                    }
                }
                gridElement {
                    items.forEach { item ->
                        selectBox(
                            name = item.name,
                            linkUrl = item.name,
                            imageUrl = item.image
                        )
                    }
                }
            }
        }
    }
}