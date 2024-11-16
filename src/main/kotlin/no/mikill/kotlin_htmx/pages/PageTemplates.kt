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
                content = "Hello"
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
            script(src = "https://unpkg.com/htmx.org@2.0.3") {}
            script(src = "https://unpkg.com/htmx.org@2.0.3/dist/ext/json-enc.js") {}
            script(src = "https://unpkg.com/htmx-ext-preload@2.0.1/preload.js") {}
            script(src = "https://unpkg.com/htmx-ext-sse@2.2.2/sse.js") {}

            style {
                rawCss(
                    """                        
                        .htmx-indicator{
                            opacity:0;
                            transition: opacity 500ms ease-in;
                        }
                        .htmx-request .htmx-indicator{
                            opacity:1
                        }
                        .htmx-request.htmx-indicator{
                            opacity:1
                        }                                        
                        
                        .box {
                            border: 1px solid red;
                            border-radius: 0.5em;
                            text-align: center;
                            padding: 1em;                    
                        }
                                            
                        section {
                            margin-bottom: 2em;
                        }
                                                                                                    
                        nav {
                            background-color: #333;
                            width: 100%;
                            border-radius: 8px;
                            font-size: 0.8em;
                            padding-left: 1em;
                            padding-right: 1em;
                            margin-bottom: 1em;
                        
                            & ul {
                                list-style: none;
                                display: flex;
                                justify-content: space-evenly;
                                align-items: center;
                                margin: 0 auto;
                                padding: 0;
                                width: 100%;
                            }
                        
                            & li {
                                display: flex;
                                align-items: center;
                                margin: 0;
                                white-space: nowrap;
                            }
                        
                            & .separator {
                                color: #666;
                            }
                        
                            & a {
                                color: white;
                                text-decoration: none;
                                font-family: Arial, sans-serif;
                                transition: color 0.3s ease;
                            }
                        
                            & a:hover {
                                color: #66c2ff;
                            }
                        }

                        .form-error {
                            color: red;
                        }
                    """.trimIndent()
                )
            }
        }
        body {            // This is inherited so means we use JSON as a default for all communication
            attributes["hx-ext"] = "json-enc"

            div {
                style = "max-width: 90vw; margin: auto;"

                // Logo
                header {
                    h1 { +"Kotlin, KTor and HTMX front end demos" }

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
                        insert(headerContent)
                    }
                }

                // Main content
                main {
                    id = "mainContent"
                    insert(template, mainSectionTemplate)
                }

                footer {
                    +"This is the footer. - Made by Anders Sveen. Check out "
                    a(href = "https://www.mikill.no") { +"https://www.mikill.no" }
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
                    #choices {
                        display: grid; /* Enables grid layout */
                        grid-template-columns: repeat(auto-fit, minmax(15em, 1fr)); /* Adjust the number of columns based on the width of the container */
                        /* Key line for responsiveness: */
                        gap: 20px; /* Adjust the spacing between items */
            
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