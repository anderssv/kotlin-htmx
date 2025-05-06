package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.rawCss

/**
 * See https://ktor.io/docs/server-html-dsl.html#templates for more information
 */
class MainTemplate<T : Template<FlowContent>>(private val template: T, val pageTitle: String) : Template<HTML> {

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
            link {
                rel = "stylesheet"
                href = "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css"
            }
            // Add Google Fonts for better typography
            link {
                rel = "stylesheet"
                href = "https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700&display=swap"
            }
            script(src = "https://www.googletagmanager.com/gtag/js?id=G-30QSF4X9PW") {}
            script {
                unsafe {
                    raw(
                        """
                          window.dataLayer = window.dataLayer || [];
                          function gtag(){dataLayer.push(arguments);}
                          gtag('js', new Date());

                          gtag('config', 'G-30QSF4X9PW');
                        """.trimIndent()
                    )
                }
            }
            // Just experimenting to see how much the HTMX JS code adds overhead
            if ((System.getenv("ENABLE_HTMX") ?: "true") == "true") {
                script(src = "https://unpkg.com/htmx.org@2.0.3") {}
                script(src = "https://unpkg.com/htmx-ext-json-enc@2.0.1/json-enc.js") {}
                script(src = "https://unpkg.com/htmx-ext-preload@2.0.1/preload.js") {}
                script(src = "https://unpkg.com/htmx-ext-sse@2.2.2/sse.js") {}
            }

            style {
                rawCss(
                    """
                        /* Custom font for better typography - works with picocss */
                        body {
                            font-family: 'Inter', sans-serif;
                        }

                        /* Primary color for headings to match our theme */
                        h1 {
                            color: #4361ee;
                        }

                        /* HTMX-specific styles for loading indicators */
                        .htmx-indicator {
                            opacity: 0;
                            transition: opacity 500ms ease-in;
                        }

                        .htmx-request .htmx-indicator {
                            opacity: 1;
                        }

                        .htmx-request.htmx-indicator {
                            opacity: 1;
                        }

                        /* Custom box component with hover effect */
                        .box {
                            border: 1px solid #ced4da;
                            border-radius: 8px;
                            text-align: center;
                            padding: 1.5em;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                            transition: transform 0.3s ease, box-shadow 0.3s ease;
                        }

                        .box:hover {
                            transform: translateY(-3px);
                            box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
                        }

                        /* Section styling for card-like appearance */
                        section {
                            margin-bottom: 2.5em;
                            padding: 1.5em;
                            border-radius: 8px;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }

                        /* Custom navigation styling */
                        nav {
                            background: linear-gradient(135deg, #4361ee, #3f37c9);
                            width: 100%;
                            border-radius: 8px;
                            font-size: 0.9em;
                            padding: 0.8em 1.5em;
                            margin-bottom: 1.5em;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);

                            & ul {
                                list-style: none;
                                display: flex;
                                flex-wrap: wrap;
                                justify-content: space-evenly;
                                align-items: center;
                                margin: 0.5em auto;
                                padding: 0;
                                width: 100%;
                                gap: 1em;
                            }

                            & li {
                                display: flex;
                                align-items: center;
                                margin: 0;
                                padding: 0;
                            }

                            & .separator {
                                color: rgba(255, 255, 255, 0.5);
                            }

                            & a {
                                color: white;
                                text-decoration: none;
                                font-weight: 500;
                                transition: all 0.3s ease;
                                padding: 0.5em 0.8em;
                                border-radius: 8px;
                            }

                            & a:hover {
                                background-color: rgba(255, 255, 255, 0.1);
                                transform: translateY(-2px);
                                text-decoration: none;
                            }
                        }

                        /* Form validation error styling */
                        .form-error {
                            color: #f44336;
                            font-size: 0.9em;
                            margin-top: 0.3em;
                        }

                        /* HTMX content update highlight effect */
                        .htmx-modified {
                            animation: highlight-fade 2s ease-out;
                        }

                        @keyframes highlight-fade {
                            from {
                                background-color: rgba(240,76,180,0.86);
                            }
                            to {
                                background-color: transparent;
                            }
                        }

                        /* Page transition animation */
                        @keyframes fadeIn {
                            from { opacity: 0; transform: translateY(10px); }
                            to { opacity: 1; transform: translateY(0); }
                        }
                    """.trimIndent()
                )
            }
        }
        body {

            div {
                style = "max-width: 1200px; margin: 2rem auto; padding: 0 1.5rem;"

                // Logo and header
                header {
                    classes = setOf("site-header")
                    style = "text-align: center; margin-bottom: 2rem;"

                    h1 {
                        style = "margin-bottom: 1rem; font-weight: 700;"
                        +"Kotlin, KTor and HTMX front end demos"
                    }

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
                            li { a(href = "/demo/form") { +"Form" } }
                        }
                    }

                    div {
                        style = "margin-top: 1.5rem;"
                        insert(headerContent)
                    }
                }

                // Main content
                main {
                    id = "mainContent"
                    style = """
                            min-height: 60vh; 
                            /* animation: fadeIn 0.5s ease-in-out; */
                        """.trimIndent()
                    insert(template, mainSectionTemplate)
                }

                footer {
                    style = "text-align: center; padding: 2rem 0; margin-top: 3rem;"
                    +"Made with ❤️ by Anders Sveen • Check out "
                    a(href = "https://www.mikill.no") { +"mikill.no" }
                }

                script {
                    unsafe {
                        raw(
                            """
                            // This script is here to highlight changes done by HTMX
                            document.body.addEventListener('htmx:afterSettle', function(evt) {
                                // The updated element is directly available in evt.detail.elt
                                const updatedElement = evt.detail.elt;
                                updatedElement.classList.add('htmx-modified');

                                // Remove the class when the animation completes
                                updatedElement.addEventListener('animationend', function() {
                                    updatedElement.classList.remove('htmx-modified');
                                }, { once: true });
                            });
                        """.trimIndent()
                        )
                    }
                }
            }
        }
    }
}

// The two below is mainly to cater for two different sub-templates
class SelectionTemplate : Template<FlowContent> {
    val selectionPagesContent = Placeholder<FlowContent>()

    override fun FlowContent.apply() {
        style {
            rawCss(
                """
                    /* Responsive grid layout for selection choices - works well with picocss */
                    #choices {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(15em, 1fr)); 
                        gap: 20px;

                        a {
                            display: block;
                        }
                    }                    
                """.trimIndent()
            )
        }
        insert(selectionPagesContent)
    }
}

/**
 * This is an empty template to allow us to enforce specifying something
 *
 * There is probably a better way to do this
 */
class EmptyTemplate : Template<FlowContent> {
    val emptyContentWrapper = Placeholder<FlowContent>()

    override fun FlowContent.apply() {
        insert(emptyContentWrapper)
    }
}
