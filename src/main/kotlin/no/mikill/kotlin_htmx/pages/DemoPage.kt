package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.util.pipeline.*
import kotlinx.html.*

class DemoPage {
    suspend fun renderPage(context: PipelineContext<Unit, ApplicationCall>) {
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
                templateContent {
                    demoContent {
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
                                // Would have included HTMX script here, but it is already included in head as it is used in other pages as well
                                +"Click me!"
                                div(classes = "htmx-indicator") {
                                    +"Loading... (Intentionally delayed for 5 seconds)"
                                }
                            }
                        }
                        section {
                            h1 { +"React Element" }
                            div {
                                id = "react-content"
                                style = "border: 1px solid red; padding: 10px; margin: 10px;"
                                script {
                                    src = "https://unpkg.com/@babel/standalone/babel.min.js"
                                }
                                script {
                                    src = "script/react-script.js"
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

    suspend fun renderForm(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
        with(pipelineContext) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                headerContent {
                    span { +"Form demo" }
                }
                templateContent {
                    demoContent {
                        script { src = "https://cdn.jsdelivr.net/gh/anderssv/formjson/src/formjson.js" }
                        form {
                            attributes["formjson"] = "true"
                            method = FormMethod.post
                            input {
                                name = "person.firstName"
                                minLength = "3"
                                required = true
                            }
                            input {
                                name = "person.lastName"
                                minLength = "3"
                                required = true
                            }
                            input {
                                name = "ok"
                                type = InputType.submit
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun saveForm(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
        with(pipelineContext) {
            val form = call.receiveParameters()
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                templateContent {
                    demoContent {
                        section {
                            h1 { +"Form save" }
                            div {
                                +"Form saved"
                            }
                            div {
                                +"Form data: "
                                pre {
                                    +form.toString()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}