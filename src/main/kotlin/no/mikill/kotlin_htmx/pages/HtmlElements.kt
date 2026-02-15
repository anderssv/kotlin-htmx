@file:OptIn(ExperimentalKtorApi::class)

package no.mikill.kotlin_htmx.pages

import io.ktor.htmx.HxSwap
import io.ktor.htmx.html.hx
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.html.A
import kotlinx.html.BODY
import kotlinx.html.FlowContent
import kotlinx.html.HtmlBlockTag
import kotlinx.html.STYLE
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.consumers.filter
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.ul
import kotlinx.html.unsafe
import no.mikill.kotlin_htmx.pages.Styles.BOX_STYLE
import no.mikill.kotlin_htmx.todo.TodoListItem
import org.intellij.lang.annotations.Language
import kotlin.time.Duration

object Styles {
    const val BOX_STYLE = "border: 1px solid red; padding: 10px; margin: 10px;"
}

object HtmlRenderUtils {
    suspend fun ApplicationCall.respondHtmlFragment(
        status: HttpStatusCode = HttpStatusCode.OK,
        block: BODY.() -> Unit,
    ) {
        val text = partialHtml(block)
        respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8), status))
    }

    fun partialHtml(block: BODY.() -> Unit): String =
        buildString {
            appendHTML().filter { if (it.tagName in listOf("html", "body")) SKIP else PASS }.html {
                body {
                    block(this)
                }
            }
        }
}

object HtmlElements {
    fun FlowContent.htmxTodolistSectionContent(
        loadDelay: Duration?,
        backendDelay: Duration,
    ) {
        section {
            h1 { +"HTMX Element" }
            div {
                attributes.hx {
                    get = "/data/todolist.html?delay=${backendDelay.inWholeSeconds}"
                    loadDelay?.let {
                        // Click is default
                        trigger = "load delay:${loadDelay.inWholeSeconds}s, click" // Default is click
                    }
                    swap = HxSwap.innerHtml
                }
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
        blockIdPrefix: String, // Sometimes included twice in a page, so this gives isolation
        todoListItems: List<TodoListItem>, // The items to display
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
            unsafe { +"document.getElementById('$blockIdPrefix-date').innerHTML = new Date().toLocaleString();" }
        }
    }

    fun HtmlBlockTag.selectBox(
        name: String,
        linkUrl: String,
        imageUrl: String,
    ) {
        a(href = linkUrl, classes = "box") {
            boostAndPreload()

            img(src = imageUrl, alt = "Choose $name") { width = "100px" }
            p { +name }
        }
    }

    private fun A.boostAndPreload() {
        // Preloading resources (not HTMX, so stays as manual attributes)
        attributes["preload"] = "mouseover"
        attributes["preload-images"] = true.toString()

        // Boosting using HTMX DSL
        attributes.hx {
            boost = true
            select = "#mainContent" // The DIV in the response that is inserted
            target = "#mainContent" // The DIV in the existing page that is replaced
            swap = HxSwap.outerHtml + " show:window:top" // Makes sure the window scrolls to the top
        }
    }

    fun STYLE.rawCss(
        @Language("CSS") css: String,
    ) {
        unsafe {
            raw(css)
        }
    }
}
