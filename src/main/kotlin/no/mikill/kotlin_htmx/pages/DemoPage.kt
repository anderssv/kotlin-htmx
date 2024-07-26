package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.html.*

class DemoPage {
    suspend fun renderPage(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate()) {
                val sourceUrl = "https://github.com/anderssv/web-playground/tree/main/combined"
                headerContent {
                    p {
                        +"This is a small test. You can see the source at "
                        a(href = sourceUrl) { +sourceUrl }
                        +" or just hit view source. 😃"
                    }
                    p { +"Loading below is staggered on purpose to show steps. Just a crude wait." }
                }
                pageContent {
                    section {
                        h1 { +"HTML Element" }
                        div {
                            style = "border: 1px solid red; padding: 10px; margin: 10px;"
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
                                    id = "html-date"
                                }
                            }
                        }
                        script {
                            +"document.getElementById('html-date').innerHTML = new Date().toLocaleString();"
                        }
                    }
                    section {
                        h1 { +"Lit Element" }
                        div {
                            style = "border: 1px solid red; padding: 10px; margin: 10px;"
                            script {
                                src = "/script/lit-script.js"
                                type = "module"
                            }
                            unsafe { raw("<my-element></my-element>") } // TODO: How is this done without unsafe?
                        }
                    }
                    section {
                        h1 { +"HTMX Element" }
                        div {
                            attributes["hx-get"] = "data/todolist.html"
                            style = "border: 1px solid red; padding: 10px; margin: 10px;"
                            script {
                                src = "https://unpkg.com/htmx.org@latest"
                            }
                            + "Click me!"
                        }
                    }
                }
            }
        }
    }
}