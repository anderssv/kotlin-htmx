package no.mikill.kotlin_htmx.pages

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import jakarta.validation.ConstraintViolation
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import kotlinx.html.*
import kotlinx.html.consumers.filter
import kotlinx.html.stream.appendHTML
import no.mikill.kotlin_htmx.application.Application
import no.mikill.kotlin_htmx.getProperty
import no.mikill.kotlin_htmx.getValueFromPath
import no.mikill.kotlin_htmx.pages.Styles.BOX_STYLE
import org.intellij.lang.annotations.Language
import kotlin.reflect.jvm.javaField

object Styles {
    const val BOX_STYLE = "border: 1px solid red; padding: 10px; margin: 10px;"
}

object HtmlElements {

    object DemoContent {
        fun FlowContent.htmxSectionContent() {
            section {
                h1 { +"HTMX Element" }
                div {
                    attributes["hx-get"] = "/data/todolist.html"
                    attributes["hx-swap"] = "innerHTML" // Default is outerHTML
                    attributes["hx-trigger"] = "load delay:1s, click" // Default is click
                    style = BOX_STYLE
                    // Would have included HTMX script here, but it is already included in head as it is used in other pages as well
                    +"Click me!"
                    div(classes = "htmx-indicator") {
                        +"Loading... (Intentionally delayed for 1 seconds)"
                    }
                }
            }
        }

        fun FlowContent.htmlSectionContent() {
            section {
                h1 { +"HTML Element" }
                div {
                    style = BOX_STYLE
                    todoListHtmlContent("html")
                }
            }
        }

        fun HtmlBlockTag.todoListHtmlContent(blockIdPrefix: String) {
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
    }

    fun HtmlBlockTag.selectBox(name: String, linkUrl: String, imageUrl: String) {
        a(href = linkUrl, classes = "box") {
            boostAndPreload()

            img(
                src = imageUrl, alt = "Choose $name"
            ) { width = "100px" }
            p { +name }
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

    fun INPUT.setConstraints(annotations: Array<Annotation>) {
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

    fun FORM.inputFieldWithValidationAndErrors(
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

    suspend fun ApplicationCall.respondHtmlFragment(
        status: HttpStatusCode = HttpStatusCode.OK,
        block: BODY.() -> Unit
    ) {
        val text = buildString {
            append("<!DOCTYPE html>\n")
            appendHTML().filter { if (it.tagName in listOf("html", "body")) SKIP else PASS }.html {
                body {
                    block(this)
                }
            }
        }
        respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8), status))
    }

    fun STYLE.rawCss(@Language("CSS") css: String) {
        unsafe {
            raw(css)
        }
    }

}