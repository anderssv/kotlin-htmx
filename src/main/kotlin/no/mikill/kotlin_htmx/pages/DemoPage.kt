package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import kotlinx.html.*
import no.mikill.kotlin_htmx.application.Application
import no.mikill.kotlin_htmx.getProperty
import no.mikill.kotlin_htmx.getValueFromPath
import kotlin.reflect.jvm.javaField

class DemoPage {
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
                                unsafe { +"document.getElementById('html-date').innerHTML = new Date().toLocaleString();" }
                            }
                        }
                        section {
                            h1 { +"HTMX Element" }
                            div {
                                attributes["hx-get"] = "/data/todolist.html"
                                attributes["hx-trigger"] = "load"
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

    suspend fun renderInputForm(
        pipelineContext: PipelineContext<Unit, ApplicationCall>,
        existingApplication: Application?,
        errors: Set<ConstraintViolation<Application>>
    ) {
        with(pipelineContext) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                headerContent {
                    span { +"Form demo" }
                }
                mainTemplateContent {
                    demoPagesContent {
                        // This is a custom script that I have written. It should be packaged as a library and published
                        // to NPM instead of just using a direct link to Github. Let me know if you want to use it and
                        // I will add it to the project.
                        script { src = "https://cdn.jsdelivr.net/gh/anderssv/formjson/src/formjson.js" }
                        form {
                            attributes["formjson"] = "true"
                            method = FormMethod.post

                            inputFieldWithValidationAndErrors(
                                existingApplication,
                                "person.firstName",
                                "First name",
                                errors
                            )
                            inputFieldWithValidationAndErrors(
                                existingApplication,
                                "person.lastName",
                                "Last name",
                                errors
                            )
                            submitInput { name = "ok" }
                        }
                    }
                }
            }
        }
    }

    private fun FORM.inputFieldWithValidationAndErrors(
        existingObject: Any?,
        propertyPath: String,
        text: String,
        errors: Set<ConstraintViolation<Application>>
    ) {
        val objectProperty = getProperty<Application>(propertyPath)
        val objectValue = existingObject?.let { getValueFromPath(it, propertyPath) }
        label {
            +"$text: "
            input {
                name = propertyPath
                value = objectValue?.toString() ?: ""
                setConstraints(objectProperty.javaField!!.annotations)
            }
            errors.filter { it.propertyPath.toString() == propertyPath }.let {
                if (it.isNotEmpty()) {
                    ul(classes = "form-error") {
                        it.map { constraintViolation -> constraintViolation.message }.forEach { message ->
                            li { +message }
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
                mainTemplateContent {
                    demoPagesContent {
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

    suspend fun renderAdminPage(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
        with(pipelineContext) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                mainTemplateContent {
                    demoPagesContent {
                        h1 { +"Admin page" }
                        div(classes = "grid") {
                            style = "grid-template-columns: 30% 1fr;"
                            style {
                                +"""
                                .grid {
                                    div {
                                        border: 1px solid red;
                                        padding: 1em;
                                    }                                
                                }
                            """.trimIndent()
                            }
                            div {
                                (0..10).forEach { item ->
                                    p {
                                        a(href = "item/$item") {
                                            attributes["hx-get"] = "item/$item"
                                            attributes["hx-target"] = "#itemPanel"
                                            +"Item $item"
                                        }
                                    }
                                }
                            }
                            div {
                                id = "itemPanel"
                                +"Choose on left"
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun INPUT.setConstraints(annotations: Array<Annotation>) {
    annotations.forEach { annotation ->
        when (annotation) {
            is NotEmpty -> required = true
            is Size -> {
                minLength = annotation.min.toString()
                maxLength = annotation.max.toString()
            }
            // Could add Pattern here as well, but purposely left out for demo reasons (we need one that is on the server too)
        }
    }
}
