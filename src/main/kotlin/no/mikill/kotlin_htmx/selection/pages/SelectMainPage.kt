@file:OptIn(ExperimentalKtorApi::class)

package no.mikill.kotlin_htmx.selection.pages

import io.ktor.htmx.HxSwap
import io.ktor.htmx.html.hx
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.routing.RoutingContext
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.section
import kotlinx.html.ul
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.integration.LookupResult
import no.mikill.kotlin_htmx.pages.HtmlElements.selectBox
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import no.mikill.kotlin_htmx.pages.MainTemplate
import no.mikill.kotlin_htmx.pages.SelectionTemplate
import no.mikill.kotlin_htmx.selection.items
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


class SelectMainPage(
    private val lookupClient: LookupClient,
) {
    private val logger = LoggerFactory.getLogger(SelectMainPage::class.java)
    private val routePath = "/select"

    private data class Search(val lookupValue: String)

    suspend fun renderMainPage(routingHandler: RoutingContext) {
        with(routingHandler) {
            call.respondHtmlTemplate(MainTemplate(template = SelectionTemplate(), "Select Main")) {
                mainSectionTemplate {
                    selectionPagesContent {
                        section {
                            div {
                                form {
                                    attributes.hx {
                                        // ext = "json-enc"  // May not be available in DSL
                                        post = "/select/search"
                                        swap = HxSwap.outerHtml
                                    }
                                    // Fall back to manual attribute for ext
                                    attributes["hx-ext"] = "json-enc"

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
                                    linkUrl = listOf(routePath, item.name).joinToString("/"),
                                    imageUrl = item.image
                                )
                            }
                        }
                        section {
                            +"You can see different demos here: "
                            ul {
                                li {
                                    a(href = "/demo/multi") { +"HTMX component together with others" }
                                }
                                li {
                                    a(href = "/demo/form") { +"Form flow handling with validations" }
                                }
                                li {
                                    a(href = "/demo/admin") { +"Simple admin page operations" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun search(context: RoutingContext) {
        with(context) {
            val search: Search = call.receive()
            call.respondHtmlFragment {
                when (val lookupResult = lookupClient.lookup(search.lookupValue)) {
                    is LookupResult.Success -> {
                        val item = items.single { it.name == lookupResult.response }

                        sleep(1.seconds.toJavaDuration())
                        call.response.header("HX-Redirect", "/select/" + item.name)
                        // Probably won't show but adding content anyway
                        div {
                            +"Found it! ${item.name}"
                        }
                    }

                    is LookupResult.NotFound ->
                        div(classes = "text-red-800") {
                            p { +"Could not locate item with '${search.lookupValue}'." }
                            a(href = routePath) { +"Try again" }
                        }


                    is LookupResult.InvalidInput ->
                        div(classes = "text-red-800") {
                            p { +lookupResult.message }
                            a(href = routePath) { +"Try again" }
                        }


                    is LookupResult.Failure -> {
                        logger.error("Lookup failed. Reason: ${lookupResult.reason}")
                        div(classes = "text-red-800") {
                            p { +"We're sorry. Something went wrong. We'll fix it ASAP." }
                            a(href = routePath) { +"Try again" }
                        }
                    }
                }
            }
        }
    }

}