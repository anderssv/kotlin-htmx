@file:OptIn(ExperimentalKtorApi::class)

package no.mikill.kotlin_htmx.selection.pages

import io.ktor.htmx.html.hx
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.response.header
import io.ktor.server.routing.RoutingContext
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.section
import no.mikill.kotlin_htmx.pages.HtmlElements.selectBox
import no.mikill.kotlin_htmx.pages.MainTemplate
import no.mikill.kotlin_htmx.pages.SelectionTemplate
import no.mikill.kotlin_htmx.selection.items

class SelectedPage {
    suspend fun renderPage(context: RoutingContext) {
        with(context) {
            call.response.header(HttpHeaders.CacheControl, CacheControl.MaxAge(maxAgeSeconds = 30).toString())
            val selected = items.single { it.name.equals(call.parameters["itemName"], ignoreCase = true) }
            call.respondHtmlTemplate(MainTemplate(template = SelectionTemplate(), "Select Selected ${selected.name}")) {
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
                            attributes.hx {
                                ext = "preload"
                            }

                            selectBox(
                                name = "Yes",
                                linkUrl = "Yes",
                                imageUrl = "/static/images/influencer.png",
                            )
                            selectBox(
                                name = "No",
                                linkUrl = "No",
                                imageUrl = "/static/images/influencer.png",
                            )
                        }
                    }
                }
            }
        }
    }
}
