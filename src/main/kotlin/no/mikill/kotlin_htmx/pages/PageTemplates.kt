package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.Placeholder
import io.ktor.server.html.Template
import io.ktor.server.html.TemplatePlaceholder
import io.ktor.server.html.insert
import kotlinx.html.FlowContent
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.footer
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.header
import kotlinx.html.id
import kotlinx.html.lang
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.nav
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.html.unsafe

/**
 * Main template for all pages in the application.
 *
 * This template provides the common HTML structure including:
 * - HTML head with meta tags, CSS, and JavaScript
 * - Header with navigation
 * - Main content area
 * - Footer
 *
 * For more information on Ktor HTML templates, see:
 * https://ktor.io/docs/server-html-dsl.html#templates
 *
 * @param template The content template to insert in the main section
 * @param pageTitle The title of the page to display in the browser tab
 */
class MainTemplate<T : Template<FlowContent>>(
    private val template: T,
    val pageTitle: String,
) : Template<HTML> {
    val mainSectionTemplate = TemplatePlaceholder<T>()
    val headerContent = Placeholder<FlowContent>()

    override fun HTML.apply() {
        lang = "en"
        attributes["data-theme"] = "light"

        head {
            title { +"HTMX and KTor <3 - $pageTitle" }
            meta { charset = "UTF-8" }
            meta {
                name = "viewport"
                content = "width=device-width, initial-scale=1"
            }
            meta {
                name = "description"
                content = "HTMX and KTor demos and examples"
            }
            link {
                rel = "icon"
                href = "/static/favicon.ico"
                type = "image/x-icon"
                sizes = "any"
            }

            // Load PicoCSS for responsive styling
            link {
                rel = "stylesheet"
                href = "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css"
            }

            // Load Google Fonts for better typography
            link {
                rel = "stylesheet"
                href = "https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=block"
            }

            // Load application styles
            link {
                rel = "stylesheet"
                href = "/css/styles.css"
            }

            // Google Analytics
            script(src = "https://www.googletagmanager.com/gtag/js?id=G-30QSF4X9PW") { defer = true }
            script {
                unsafe {
                    raw(
                        """
                        window.dataLayer = window.dataLayer || [];
                        function gtag(){dataLayer.push(arguments);}
                        gtag('js', new Date());

                        gtag('config', 'G-30QSF4X9PW');
                        """.trimIndent(),
                    )
                }
            }

            // Load HTMX and its extensions
            if ((System.getenv("ENABLE_HTMX") ?: "true") == "true") {
                script(src = "https://unpkg.com/htmx.org@2.0.3") { defer = true }
                script(src = "https://unpkg.com/htmx-ext-json-enc@2.0.1/json-enc.js") { defer = true }
                script(src = "https://unpkg.com/htmx-ext-preload@2.0.1/preload.js") { defer = true }
                script(src = "https://unpkg.com/htmx-ext-sse@2.2.2/sse.js") { defer = true }
            }
        }
        body {
            // Main container with responsive width
            div {
                style = "max-width: 1200px; margin: 2rem auto; padding: 0 1.5rem;"

                // Header section with site title and navigation
                header {
                    classes = setOf("site-header")
                    style = "text-align: center; margin-bottom: 2rem;"

                    h1 {
                        style = "margin-bottom: 1rem; font-weight: 700;"
                        +"Kotlin, KTor and HTMX front end demos"
                    }

                    // Main navigation
                    nav {
                        ul {
                            li { a(href = "/") { +"Home" } }
                            li { span("separator") { +"🚀" } }
                            li { a(href = "/demo/htmx/checkboxes") { +"Checkboxes" } }
                            li { span("separator") { +"🚀" } }
                            li { a(href = "/select") { +"SPA Emulation" } }
                            li { span("separator") { +"🚀" } }
                            li { a(href = "/demo/admin") { +"Admin demo" } }
                            li { span("separator") { +"🚀" } }
                            li { a(href = "/person/register") { +"Form Demo" } }
                        }
                    }

                    // Optional header content from page
                    div {
                        style = "margin-top: 1.5rem;"
                        insert(headerContent)
                    }
                }

                // Main content area
                main {
                    id = "mainContent"
                    style =
                        """
                        min-height: 60vh; 
                        /* Uncomment to enable page transition animation:
                           animation: fadeIn 0.5s ease-in-out; */
                        """.trimIndent()
                    insert(template, mainSectionTemplate)
                }

                // Footer
                footer {
                    style = "text-align: center; padding: 2rem 0; margin-top: 3rem;"
                    +"Made with ❤️ by Anders Sveen • Check out "
                    a(href = "https://www.mikill.no") { +"mikill.no" }
                }

                // HTMX highlight effect for updated elements
                script {
                    unsafe {
                        raw(
                            """
                            // This script highlights elements that have been updated by HTMX
                            document.body.addEventListener('htmx:afterSettle', function(evt) {
                                // The updated element is directly available in evt.detail.elt
                                const updatedElement = evt.detail.elt;
                                updatedElement.classList.add('htmx-modified');

                                // Remove the class when the animation completes
                                updatedElement.addEventListener('animationend', function() {
                                    updatedElement.classList.remove('htmx-modified');
                                }, { once: true });
                            });
                            """.trimIndent(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Template for selection pages that displays content in a responsive grid layout.
 * Used for pages where users need to select from multiple options.
 */
class SelectionTemplate : Template<FlowContent> {
    val selectionPagesContent = Placeholder<FlowContent>()

    override fun FlowContent.apply() {
        insert(selectionPagesContent)
    }
}

/**
 * An empty template that serves as a placeholder for content.
 *
 * This template is used to enforce the template pattern even when a page
 * doesn't need specific template functionality beyond the main template.
 * It provides a consistent way to insert content into the main template.
 */
class EmptyTemplate : Template<FlowContent> {
    val emptyContentWrapper = Placeholder<FlowContent>()

    override fun FlowContent.apply() {
        insert(emptyContentWrapper)
    }
}
