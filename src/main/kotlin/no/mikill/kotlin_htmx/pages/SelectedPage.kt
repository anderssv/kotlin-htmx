package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.section
import no.mikill.kotlin_htmx.items
import no.mikill.kotlin_htmx.pages.HtmlElements.selectBox


class SelectedPage {
    suspend fun renderPage(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            val selected = items.single { it.name.equals(call.parameters["itemName"], ignoreCase = true) }
            call.respondHtmlTemplate(MainTemplate(template = SelectionTemplate())) {
                templateContent {
                    selectionContent {
                        val name =
                            "${selected.name} - Yes and No below has not been implemented and will generate an error"
                        section(classes = "box") {
                            img(src = selected.image) {
                                alt = "Chosen $name"
                            }
                            p { +name }
                        }
                        section {
                            id = "choices"
                            attributes["hx-ext"] = "preload"

                            selectBox(
                                name = "Yes",
                                linkUrl = "Yes",
                                imageUrl = "https://image.com/something"
                            )
                            selectBox(
                                name = "No",
                                linkUrl = "No",
                                imageUrl = "https://image.com/something"
                            )
                        }
                    }
                }
            }
        }
    }
}