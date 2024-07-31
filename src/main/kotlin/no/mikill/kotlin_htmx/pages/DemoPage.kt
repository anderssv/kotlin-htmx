package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import kotlinx.html.*
import no.mikill.kotlin_htmx.application.Application
import no.mikill.kotlin_htmx.resolveProperty

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

    suspend fun renderInputForm(
        pipelineContext: PipelineContext<Unit, ApplicationCall>,
        existingApplication: Application,
        errors: Set<ConstraintViolation<Application>>
    ) {
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
                            val firstNamePropertyAndValue =
                                resolveProperty<String>(existingApplication, "person.firstName")
                            input {
                                name = "person.firstName"
                                firstNamePropertyAndValue.getJavaFieldAnnotations()?.forEach { annotation ->
                                    println(annotation)
                                    when (annotation) {
                                        is NotEmpty -> required = true
                                        is Size -> {
                                            minLength = annotation.min.toString()
                                            maxLength = annotation.max.toString()
                                        }
                                    }
                                }
                                value = firstNamePropertyAndValue.value ?: ""
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

    suspend fun renderFormSaved(
        pipelineContext: PipelineContext<Unit, ApplicationCall>,
        existingApplication: Application
    ) {
        with(pipelineContext) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                templateContent {
                    demoContent {
                        section {
                            h1 { +"Form save" }
                            div {
                                +"Form saved"
                            }
                            div {
                                +"Application object data: "
                                dl {
                                    dt { +"First name" }
                                    dd { +existingApplication.person.firstName }
                                    dt { +"Last name" }
                                    dd { +existingApplication.person.lastName }
                                    dt { +"Comments" }
                                    dd { +existingApplication.comments }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}