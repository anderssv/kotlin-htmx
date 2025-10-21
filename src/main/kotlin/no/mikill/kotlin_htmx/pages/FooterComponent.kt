package no.mikill.kotlin_htmx.pages

import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.footer
import kotlinx.html.style

fun FlowContent.footerComponent() {
    footer {
        style = "text-align: center; padding: 2rem 0; margin-top: 3rem;"
        +"Made with ❤️ by Anders Sveen • Check out "
        a(href = "https://www.mikill.no") { +"mikill.no" }
    }
}
