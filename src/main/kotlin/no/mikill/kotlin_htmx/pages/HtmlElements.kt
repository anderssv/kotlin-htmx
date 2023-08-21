package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.css.*
import kotlinx.html.*
import kotlinx.html.consumers.filter
import kotlinx.html.stream.createHTML

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
            // Preloading resources
            attributes["preload"] = "mouseover"
            attributes["preload-images"] = true.toString()

            // Boosting
            attributes["hx-boost"] = true.toString()
            attributes["hx-target"] = "#mainContent"
            attributes["hx-select"] = "#mainContent"
            attributes["hx-swap"] = "outerHTML"

            div(classes = "border drop-shadow-lg border-gray-400 rounded-lg bg-white p-2 h-full bg-white overflow-hidden hover:border-indigo-100") {
                img(
                    src = imageUrl, classes = "object-cover mx-auto max-h-56", alt = name
                )
                div(classes = "p-4") {
                    p(classes = "text-center") {
                        +name
                    }
                }
            }
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

    fun HtmlBlockTag.gridElement(
        gridBlocks: HtmlBlockTag.() -> Unit
    ) {
        section(classes = "mt-4 mb-4 w-full grid gap-4 sm:grid-cols-2 xl:grid-cols-4") {
            attributes["hx-ext"] = "preload"

            gridBlocks.invoke(this)
        }
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.respondFullPage(
        localStyle: String? = null,
        contentBlock: MAIN.() -> Unit
    ) {
        call.respondHtml {
            lang = "en"
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
                // TODO But really we should load the full scripts when booting the server, then just inject it here
                // Tailwind specifically says we should not use the JS, but PostCSS to be production ready, but I don't think we will run into any problems
                script(src = "https://cdn.tailwindcss.com") { }
                script(src = "https://unpkg.com/htmx.org@1.9.2") { }
                script(src = "https://unpkg.com/htmx.org/dist/ext/json-enc.js") { }
                script(src = "https://unpkg.com/htmx.org/dist/ext/preload.js") {}

                style {
                    unsafe {
                        raw(
                            CSSBuilder().apply {
                                overflowY = Overflow.scroll
                            }.toString()
                        )
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

                // Logo
                header {
                    img(classes = "w-auto h-28 align-center mx-auto", src = "/static/images/logo.png") {
                        alt = "Logo"
                    }
                }

                // Main content
                div(classes = "w-full min-h-full p-20 items-center flex flex-col") {
                    main(classes = "w-full max-w-screen-2xl flex flex-col items-center") {
                        id = "mainContent"
                        contentBlock.invoke(this)
                    }
                }
            }
        }
    }

}