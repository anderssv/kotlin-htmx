package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.respondHtmlFragment

class AdminPage {
    suspend fun renderAdminPage(pipelineContext: PipelineContext<Unit, ApplicationCall>) {
        with(pipelineContext) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                mainTemplateContent {
                    demoPagesContent {
                        h1 { +"Admin page" }
                        div(classes = "grid") {
                            style = "grid-template-columns: 30% 1fr;"
                            style {
                                +"""
                                .grid {
                                    div {
                                        border: 1px solid red;
                                        padding: 1em;
                                    }                                
                                }
                            """.trimIndent()
                            }
                            div {
                                (0..10).forEach { item ->
                                    p {
                                        a(href = "item/$item") {
                                            attributes["hx-get"] = "item/$item"
                                            attributes["hx-target"] = "#itemPanel"
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

    suspend fun renderItemResponse(pipelineContext: PipelineContext<Unit, ApplicationCall>, itemId: Int) {
        with(pipelineContext) {
            call.respondHtmlFragment {
                div {
                    +"Item $itemId"
                }
            }
        }
    }

}