package no.mikill.kotlin_htmx.pages

import kotlinx.html.*
import kotlinx.html.consumers.filter
import kotlinx.html.stream.createHTML

object HtmlElements {

    fun HtmlBlockTag.selectBox(name: String, linkUrl: String, imageUrl: String) {
        a(href = linkUrl, classes = "box") {
            boostAndPreload()

            img(
                src = imageUrl, alt = "Choose $name"
            )
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
}

fun htmlFragment(fragment: HtmlBlockTag.() -> Unit): String {
    return createHTML().filter {
        if (it.tagName in listOf("html", "body")) SKIP else PASS
    }.html {
        body {
            fragment(this)
        }
    }
}