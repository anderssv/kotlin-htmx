package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import kotlinx.html.consumers.filter
import kotlinx.html.stream.createHTML
import org.intellij.lang.annotations.Language

object HtmlElements {
    fun HtmlBlockTag.selectedBox(name: String, imageUrl: String, displayName: Boolean = false) {
        div(classes = "w-full max-w-md border drop-shadow-lg border-gray-400 rounded-lg bg-white p-2") {
            img(src = imageUrl, classes = "object-cover mx-auto max-h-56") {
                alt = name
            }
            if (displayName) p(classes = "text-center py-2") { +name }
        }
    }

    fun HtmlBlockTag.selectBox(name: String, linkUrl: String, imageUrl: String) {
        a(href = linkUrl) {
            boostAndPreload()

            img(
                src = imageUrl, alt = name
            )
            p {
                +name
            }
        }
    }

    private fun A.boostAndPreload() {
        // Preloading resources
        attributes["preload"] = "mouseover"
        attributes["preload-images"] = true.toString()

        // Boosting
        attributes["hx-boost"] = true.toString()
        attributes["hx-target"] = "#mainContent"
        attributes["hx-select"] = "#mainContent"
        attributes["hx-swap"] = "outerHTML"
    }
}

fun htmlFragment(fragment: HtmlBlockTag.() -> Unit): String {
    return createHTML().filter {
        if (it.tagName in listOf("html", "body")) SKIP else PASS
    }.html {
        body {
            fragment(this)
        }
    }
}

// TODO Move to templating
suspend fun PipelineContext<Unit, ApplicationCall>.respondFullPage(
    localStyle: String? = null,
    contentBlock: MAIN.() -> Unit
) {
    call.respondHtml {
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
                            border: 1px solid red;
                            border-radius: 0.5em;
                            text-align: center;
                            padding: 1em;
                        }
                    }
                """.trimIndent()

            style {
                unsafe {
                    raw(globalStyle)
                }
            }
            if (localStyle != null) style {
                unsafe {
                    raw(localStyle)
                }
            }
        }
        body {
            // This is inherited so means we use JSON as a default for all communication
            attributes["hx-ext"] = "json-enc"

            div {
                style = "max-width: 90vw; margin: auto;"

                // Logo
                header {
                    img(src = "/static/images/logo.png") {
                        alt = "Logo"
                    }
                }

                // Main content
                main {
                    id = "mainContent"
                    contentBlock.invoke(this)
                }
            }
        }
    }
}