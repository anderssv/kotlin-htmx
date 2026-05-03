package no.mikill.kotlin_htmx.todo

import kotlinx.html.FlowContent
import kotlinx.html.HtmlBlockTag
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.section
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.ul
import kotlinx.html.unsafe
import no.mikill.kotlin_htmx.pages.BOX_STYLE

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
    blockIdPrefix: String,
    todoListItems: List<TodoListItem>,
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
