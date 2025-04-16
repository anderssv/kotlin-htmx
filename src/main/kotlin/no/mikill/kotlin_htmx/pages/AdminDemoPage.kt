package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.rawCss
import no.mikill.kotlin_htmx.pages.HtmlRenderUtils.respondHtmlFragment
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds

class AdminDemoPage {

    suspend fun renderAdminPage(pipelineContext: RoutingContext) {
        with(pipelineContext) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Admin")) {
                mainSectionTemplate {
                    emptyContentWrapper {
                        h1 { +"Admin page" }
                        p(classes = "htmx-indicator") {
                            id = "loader"
                            +"Loading..."
                        }
                        style {
                            rawCss(
                                """
                                    /* Grid item styling with picocss-friendly colors */
                                    .grid > div {
                                        border: 1px solid #ced4da;
                                        padding: 1em;
                                        border-radius: 8px;
                                    }                                
                                """.trimIndent()
                            )
                        }
                        div(classes = "grid") {
                            style = "grid-template-columns: 30% 1fr;"
                            div {
                                (0..10).forEach { item ->
                                    p {
                                        a(href = "item/$item") {
                                            attributes["hx-get"] = "item/$item"
                                            attributes["hx-target"] = "#itemPanel"
                                            attributes["hx-indicator"] = "#loader"
                                            +"Item $item"
                                        }
                                    }
                                }
                            }
                            div {
                                id = "itemPanel"
                                +"Choose on left"
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun renderItemResponse(pipelineContext: RoutingContext, itemId: Int) {
        delay(5.seconds)
        with(pipelineContext) {
            call.respondHtmlFragment {
                div {
                    +"Item $itemId"
                }
            }
        }
    }

}
