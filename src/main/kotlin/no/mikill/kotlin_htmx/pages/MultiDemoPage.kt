package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import kotlin.collections.set

class MultiDemoPage {
    suspend fun renderMultiJsPage(context: PipelineContext<Unit, ApplicationCall>) {
        val boxStyle = "border: 1px solid red; padding: 10px; margin: 10px;"
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                headerContent {
                    p {
                        +"This is a small test. You can see the source at: "
                        ul {
                            li { a(href = "https://github.com/anderssv/web-playground/tree/main/combined") { +"Pure HTML source (same as view source)" } }
                            li { a(href = "https://github.com/anderssv/kotlin-htmx/blob/main/src/main/kotlin/no/mikill/kotlin_htmx/pages/DemoPage.kt") { +"Kotlin + KTor source" } }
                        }
                        +"Or just hit view source. 😃"
                    }
                    p { +"Loading below is staggered on purpose to show steps. Just a crude wait." }
                }
                mainTemplateContent {
                    demoPagesContent {
                        section {
                            h1 { +"HTML Element" }
                            div {
                                style = boxStyle
                                todoListHtml("html")
                            }
                        }
                        section {
                            h1 { +"HTMX Element" }
                            div {
                                attributes["hx-get"] = "/data/todolist.html"
                                attributes["hx-trigger"] = "load delay:1s"
                                attributes["hx-swap"] = "innerHTML"
                                style = boxStyle
                                // Would have included HTMX script here, but it is already included in head as it is used in other pages as well
                                +"Click me!"
                                div(classes = "htmx-indicator") {
                                    +"Loading... (Intentionally delayed for 1 seconds)"
                                }
                            }
                        }
                        section {
                            h1 { +"Lit Element" }
                            div {
                                style = boxStyle
                                script {
                                    src = "/script/lit-script.js"
                                    type = "module"
                                }
                                unsafe { raw("<my-element></my-element>") } // TODO: How is this done without unsafe?
                            }
                        }
                        section {
                            h1 { +"React Element" }
                            div {
                                id = "react-content"
                                style = boxStyle
                                script {
                                    src = "https://unpkg.com/@babel/standalone/babel.min.js"
                                }
                                script {
                                    src = "/script/react-script.js"
                                    type = "text/babel"
                                    attributes["data-type"] = "module"
                                }
                                +"React not loaded"
                            }
                        }
                    }
                }
            }
        }
    }

}

fun HtmlBlockTag.todoListHtml(blockIdPrefix: String) {
    h1 { +"Todo List" }
    ul {
        id = "todo-list"
        li { +"Buy milk" }
        li { +"Buy bread" }
        li { +"Buy eggs" }
        li { +"Buy butter" }
    }
    p {
        span {
            id = "$blockIdPrefix-date"
        }
    }
    script {
        unsafe { +"document.getElementById('${blockIdPrefix}-date').innerHTML = new Date().toLocaleString();" }
    }
}

