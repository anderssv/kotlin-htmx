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
import no.mikill.kotlin_htmx.todo.TodoListItem
import org.intellij.lang.annotations.Language
import kotlin.reflect.jvm.javaField
import kotlin.time.Duration

object Styles {
    const val BOX_STYLE = "border: 1px solid red; padding: 10px; margin: 10px;"
}

object HtmlRenderUtils {
    suspend fun ApplicationCall.respondHtmlFragment(
        status: HttpStatusCode = HttpStatusCode.OK,
        block: BODY.() -> Unit
    ) {
        val text = partialHtml(block)
        respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8), status))
    }

    fun partialHtml(block: BODY.() -> Unit): String = buildString {
        appendHTML().filter { if (it.tagName in listOf("html", "body")) SKIP else PASS }.html {
            body {
                block(this)
            }
        }
    }
}


object FormUtils {
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
}

object HtmlElements {

    fun FlowContent.htmxTodolistSectionContent(loadDelay: Duration?, backendDelay: Duration) {
        section {
            h1 { +"HTMX Element" }
            div {
                attributes["hx-get"] = "/data/todolist.html?delay=${backendDelay.inWholeSeconds}"
                loadDelay?.let {
                    // Click is default
                    attributes["hx-trigger"] = "load delay:${loadDelay.inWholeSeconds}s, click" // Default is click
                }
                attributes["hx-swap"] = "innerHTML"
                style = BOX_STYLE
                // Would have included HTMX script here, but it is already included in the header as it is used in other pages as well
                +"Click me! ${if (loadDelay != null) " (Will automatically load after ${loadDelay.inWholeSeconds} seconds)" else ""}"
                div(classes = "htmx-indicator") {
                    img(src = "/static/images/loading.gif") { style = "height: 1em;" }
                    span {
                        style = "margin-left: 0.5em;"
                        +"Loading... (Intentionally delayed ${backendDelay.inWholeSeconds} seconds on the back end)"
                    }
                }
            }
        }
    }

    fun FlowContent.htmlTodolistSectionContent(todoListItems: List<TodoListItem>) {
        section {
            h1 { +"HTML Element" }
            div {
                style = BOX_STYLE
                todoListHtmlContent("html", todoListItems)
            }
        }
    }

    fun HtmlBlockTag.todoListHtmlContent(
        blockIdPrefix: String, // Sometimes included twice in a page, so this gives isloation
        todoListItems: List<TodoListItem> // The items to display
    ) {
        h1 { +"Todo List" }
        ul {
            id = "todo-list"
            todoListItems.forEach {
                li { +it.title }
            }
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

    fun HtmlBlockTag.selectBox(name: String, linkUrl: String, imageUrl: String) {
        a(href = linkUrl, classes = "box") {
            boostAndPreload()

            img(src = imageUrl, alt = "Choose $name") { width = "100px" }
            p { +name }
        }
    }

    private fun A.boostAndPreload() {
        // Preloading resources
        attributes["preload"] = "mouseover"
        attributes["preload-images"] = true.toString()

        // Boosting
        attributes["hx-boost"] = true.toString()
        attributes["hx-select"] = "#mainContent" // The DIV in the response that is inserted
        attributes["hx-target"] = "#mainContent" // The DIV in the existing page that is replaced
        //attributes["hx-swap"] = "outerHTML"
        attributes["hx-swap"] = "outerHTML show:window:top" // Makes sure the window scrolls to the top
    }

    fun STYLE.rawCss(@Language("CSS") css: String) {
        unsafe {
            raw(css)
        }
    }

}