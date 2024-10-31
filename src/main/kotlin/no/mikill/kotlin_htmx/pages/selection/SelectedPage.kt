package no.mikill.kotlin_htmx.pages.selection

import io.ktor.server.html.*
import io.ktor.server.routing.RoutingContext
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.section
import no.mikill.kotlin_htmx.items
import no.mikill.kotlin_htmx.pages.HtmlElements.selectBox
import no.mikill.kotlin_htmx.pages.MainTemplate
import no.mikill.kotlin_htmx.pages.SelectionTemplate


class SelectedPage {
    suspend fun renderPage(context: RoutingContext) {
        with(context) {
            val selected = items.single { it.name.equals(call.parameters["itemName"], ignoreCase = true) }
            call.respondHtmlTemplate(MainTemplate(template = SelectionTemplate())) {
                mainSectionTemplate {
                    selectionPagesContent {
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
                                imageUrl = "/static/images/influencer.png"
                            )
                            selectBox(
                                name = "No",
                                linkUrl = "No",
                                imageUrl = "/static/images/influencer.png"
                            )
                        }
                    }
                }
            }
        }
    }
}