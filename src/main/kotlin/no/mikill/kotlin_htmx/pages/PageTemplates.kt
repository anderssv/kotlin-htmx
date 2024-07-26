package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import kotlinx.html.*
import org.intellij.lang.annotations.Language

/**
 * See https://ktor.io/docs/server-html-dsl.html#templates for more information
 */
class MainTemplate(@Language("CSS") private val localStyle: String? = null) : Template<HTML> {
    val pageContent = Placeholder<FlowContent>()
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
            script(src = "https://unpkg.com/htmx.org@1.9.2") { }
            script(src = "https://unpkg.com/htmx.org/dist/ext/json-enc.js") { }
            script(src = "https://unpkg.com/htmx.org/dist/ext/preload.js") {}

            @Language("CSS")
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
                    
                    .box {
                        border: 1px solid red;
                        border-radius: 0.5em;
                        text-align: center;
                        padding: 1em;                    
                    }
                                        
                    section {
                        margin-bottom: 2em;
                    }
                """.trimIndent()

            style {
                unsafe {
                    +globalStyle
                }
            }
            if (localStyle != null) style {
                unsafe {
                    +localStyle
                }
            }
        }
        body {            // This is inherited so means we use JSON as a default for all communication
            attributes["hx-ext"] = "json-enc"

            div {
                style = "max-width: 90vw; margin: auto;"

                // Logo
                header {
                    img(src = "/static/images/logo.png") {
                        width = "100px"
                        height = "100px"
                        alt = "Logo"
                    }
                    div {
                        insert(headerContent)
                    }
                }

                // Main content
                main {
                    id = "mainContent"
                    insert(pageContent)
                }
            }
        }
    }
}