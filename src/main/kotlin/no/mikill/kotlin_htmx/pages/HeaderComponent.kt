package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.Placeholder
import io.ktor.server.html.insert
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.header
import kotlinx.html.li
import kotlinx.html.nav
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.ul

fun FlowContent.headerComponent(headerContent: Placeholder<FlowContent>? = null) {
    header {
        classes = setOf("site-header")
        style = "text-align: center; margin-bottom: 2rem;"

        h1 {
            style = "margin-bottom: 1rem; font-weight: 700;"
            +"Kotlin, KTor and HTMX front end demos"
        }

        nav {
            ul {
                li { a(href = "/") { +"Home" } }
                li { span("separator") { +"🚀" } }
                li { a(href = "/demo/htmx/checkboxes") { +"Checkboxes" } }
                li { span("separator") { +"🚀" } }
                li { a(href = "/select") { +"SPA Emulation" } }
                li { span("separator") { +"🚀" } }
                li { a(href = "/demo/admin") { +"Admin demo" } }
                li { span("separator") { +"🚀" } }
                li { a(href = "/person/register") { +"Form Demo" } }
            }
        }

        if (headerContent != null) {
            div {
                style = "margin-top: 1.5rem;"
                insert(headerContent)
            }
        }
    }
}
