package no.mikill.kotlin_htmx.todo

import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.routing.RoutingContext
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.style
import kotlinx.html.ul
import kotlinx.html.unsafe
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.HtmlElements.htmlTodolistSectionContent
import no.mikill.kotlin_htmx.pages.HtmlElements.htmxTodolistSectionContent
import no.mikill.kotlin_htmx.pages.MainTemplate
import no.mikill.kotlin_htmx.pages.Styles.BOX_STYLE
import kotlin.time.Duration.Companion.seconds

/**
 * Demo page that showcases multiple frontend frameworks working together on a single page.
 *
 * This class demonstrates how to integrate different frontend technologies:
 * - Plain HTML
 * - HTMX for dynamic content
 * - Lit Element (Web Components)
 * - React
 *
 * Each framework implements the same todolist functionality, allowing for
 * comparison of different approaches to the same problem.
 */
class MultiTodoDemoPage {
    /**
     * Renders the multi-framework demo page with all four implementations.
     *
     * @param context The routing context for the request
     */
    suspend fun renderMultiJsPage(
        context: RoutingContext,
        todoListItems: List<TodoListItem>,
    ) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Multi frameworks demo")) {
                headerContent {
                    p {
                        +"This is a small test. You can see the source at: "
                        ul {
                            li {
                                a(
                                    href = "https://github.com/anderssv/web-playground/tree/main/combined",
                                ) { +"Pure HTML source (same as view source)" }
                            }
                            li {
                                a(
                                    href = "https://github.com/anderssv/kotlin-htmx/blob/main/src/main/kotlin/no/mikill/kotlin_htmx/todo/MultiTodoDemoPage.kt",
                                ) {
                                    +"Kotlin + KTor source"
                                }
                            }
                        }
                        +"Or just hit view source. 😃"
                    }
                    p { +"Loading below is staggered on purpose to show steps. Just a crude wait." }
                }
                mainSectionTemplate {
                    emptyContentWrapper {
                        // Section 1: Plain HTML implementation of the todolist
                        // This uses server-side rendering with no JavaScript
                        htmlTodolistSectionContent(todoListItems)

                        // Section 2: HTMX implementation of the todolist
                        // This uses HTMX for dynamic content loading with minimal JavaScript
                        // - loadDelay: 5 seconds client-side delay to demonstrate staggered loading
                        // - backendDelay: 5 seconds simulated delay on the backend
                        htmxTodolistSectionContent(loadDelay = 5.seconds, backendDelay = 5.seconds)

                        // Section 3: Lit Element implementation (Web Components)
                        // This uses modern Web Components for a reusable custom element
                        section {
                            h1 { +"Lit Element" }
                            div {
                                style = BOX_STYLE
                                script {
                                    src = "/script/lit-script.js"
                                    type = "module"
                                }
                                // Custom element defined in the lit-script.js file
                                // Note: Using unsafe is necessary for custom elements in kotlinx.html
                                unsafe { raw("<my-element></my-element>") } // TODO: How is this done without unsafe?
                            }
                        }

                        // Section 4: React implementation
                        // This uses React with Babel for JSX transformation
                        section {
                            h1 { +"React Element" }
                            div {
                                id = "react-content" // Target div for React to render into
                                style = BOX_STYLE
                                script {
                                    // Babel is needed for JSX transformation
                                    src = "https://unpkg.com/@babel/standalone/babel.min.js"
                                }
                                script {
                                    // React script that will replace the "React not loaded" text
                                    src = "/script/react-script.js"
                                    type = "text/babel"
                                    attributes["data-type"] = "module"
                                }
                                +"React not loaded" // Fallback text if React fails to load
                            }
                        }
                    }
                }
            }
        }
    }
}
