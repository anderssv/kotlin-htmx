package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.integration.LookupResult
import no.mikill.kotlin_htmx.items
import no.mikill.kotlin_htmx.pages.HtmlElements.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.HtmlElements.selectBox
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class MainPage(
    private val lookupClient: LookupClient,
) {
    private val logger = LoggerFactory.getLogger(MainPage::class.java)

    private data class Search(val lookupValue: String)

    suspend fun search(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            val search: Search = call.receive()
            call.respondHtmlFragment {
                when (val lookupResult = lookupClient.lookup(search.lookupValue)) {
                    is LookupResult.Success -> {
                        val item = items.single { it.name == lookupResult.response }

                        sleep(1.seconds.toJavaDuration())
                        call.response.header("HX-Redirect", item.name)
                        // Probably won't show but adding content anyway
                        div {
                            +"Found it! ${item.name}"
                        }
                    }

                    is LookupResult.NotFound ->
                        div(classes = "text-red-800") {
                            p { +"Could not locate item with '${search.lookupValue}'." }
                            a(href = "/") { +"Try again" }
                        }


                    is LookupResult.InvalidInput ->
                        div(classes = "text-red-800") {
                            p { +lookupResult.message }
                            a(href = "/") { +"Try again" }
                        }


                    is LookupResult.Failure -> {
                        logger.error("Lookup failed. Reason: ${lookupResult.reason}")
                        div(classes = "text-red-800") {
                            p { +"We're sorry. Something went wrong. We'll fix it ASAP." }
                            a(href = "/") { +"Try again" }
                        }
                    }
                }
            }
        }
    }

    suspend fun renderMainPage(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = SelectionTemplate())) {
                mainTemplateContent {
                    selectionPagesContent {
                        section {
                            div {
                                form {
                                    attributes["hx-post"] = "/search"
                                    attributes["hx-swap"] = "outerHTML"

                                    div(classes = "htmx-indicator") {
                                        id = "formLoading"
                                        +"Searching... (Intentionally delayed for 1 seconds)"
                                    }

                                    div {
                                        input {
                                            type = InputType.text
                                            name = "lookupValue"
                                            attributes["aria-label"] = "Value"
                                            required = true
                                        }
                                        button {
                                            attributes["aria-label"] = "Check"
                                            +"Check"
                                        }
                                    }
                                    p { +"Input one of the item names below, anything else will generate an not found error" }
                                    p {
                                        +"The source and description of the code for this site: "
                                        a("https://github.com/anderssv/kotlin-htmx/blob/main/Readme.md") {
                                            +"https://github.com/anderssv/kotlin-htmx/blob/main/Readme.md"
                                        }
                                    }
                                }
                            }
                        }
                        section {
                            id = "choices"
                            attributes["hx-ext"] = "preload"

                            items.forEach { item ->
                                selectBox(
                                    name = item.name,
                                    linkUrl = item.name,
                                    imageUrl = item.image
                                )
                            }
                        }
                        section {
                            +"You can see a different demo "
                            a(href = "/demo") { +"here" }
                            +" that showcases some use of React and Lit too"
                        }
                    }
                }
            }
        }
    }
}