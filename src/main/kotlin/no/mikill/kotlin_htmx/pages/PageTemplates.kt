package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import kotlinx.html.*
import org.intellij.lang.annotations.Language

/**
 * See https://ktor.io/docs/server-html-dsl.html#templates for more information
 */
class MainTemplate<T : Template<FlowContent>>(private val template: T) : Template<HTML> {

    val mainTemplateContent = TemplatePlaceholder<T>()
    val headerContent = Placeholder<FlowContent>()

    override fun HTML.apply() {
        lang = "en"
        attributes["data-theme"] = "light"

        head {
            title { +"HTMX and KTor <3" }
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
            script(src = "https://unpkg.com/htmx.org@1.9.12") { }
            script(src = "https://unpkg.com/htmx.org/dist/ext/json-enc.js") { }
            script(src = "https://unpkg.com/htmx.org/dist/ext/preload.js") { }

            @Language("CSS") // Separated to variable to make IDEA syntax highlighting work
            val globalStyle =
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
                                        
                    .form-error {
                        color: red;
                    }
                """.trimIndent()

            style {
                unsafe {
                    +globalStyle
                }
            }
        }
        body {            // This is inherited so means we use JSON as a default for all communication
            attributes["hx-ext"] = "json-enc"

            div {
                style = "max-width: 90vw; margin: auto;"

                // Logo
                header {
                    h1 { +"Kotlin + KTor front end demos" }
                    div {
                        insert(headerContent)
                    }
                }

                // Main content
                main {
                    id = "mainContent"
                    insert(template, mainTemplateContent)
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
        insert(selectionPagesContent)
    }
}

class DemoTemplate : Template<FlowContent> {
    val demoPagesContent = Placeholder<FlowContent>()

    override fun FlowContent.apply() {
        insert(demoPagesContent)
    }
}