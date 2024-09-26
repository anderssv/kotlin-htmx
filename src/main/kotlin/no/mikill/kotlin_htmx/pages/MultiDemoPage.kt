package no.mikill.kotlin_htmx.pages

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.util.pipeline.*
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.htmlSectionContent
import no.mikill.kotlin_htmx.pages.HtmlElements.DemoContent.htmxSectionContent
import no.mikill.kotlin_htmx.pages.Styles.BOX_STYLE
import kotlin.collections.set

class MultiDemoPage {

    suspend fun renderMultiJsPage(context: PipelineContext<Unit, ApplicationCall>) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = DemoTemplate())) {
                headerContent {
                    p {
                        +"This is a small test. You can see the source at: "
                        ul {
                            li { a(href = "https://github.com/anderssv/web-playground/tree/main/combined") { +"Pure HTML source (same as view source)" } }
                            li { a(href = "https://github.com/anderssv/kotlin-htmx/blob/main/src/main/kotlin/no/mikill/kotlin_htmx/pages/DemoPage.kt") { +"Kotlin + KTor source" } }
                        }
                        +"Or just hit view source. 😃"
                    }
                    p { +"Loading below is staggered on purpose to show steps. Just a crude wait." }
                }
                mainTemplateContent {
                    demoPagesContent {
                        htmlSectionContent()
                        htmxSectionContent()
                        section {
                            h1 { +"Lit Element" }
                            div {
                                style = BOX_STYLE
                                script {
                                    src = "/script/lit-script.js"
                                    type = "module"
                                }
                                unsafe { raw("<my-element></my-element>") } // TODO: How is this done without unsafe?
                            }
                        }
                        section {
                            h1 { +"React Element" }
                            div {
                                id = "react-content"
                                style = BOX_STYLE
                                script {
                                    src = "https://unpkg.com/@babel/standalone/babel.min.js"
                                }
                                script {
                                    src = "/script/react-script.js"
                                    type = "text/babel"
                                    attributes["data-type"] = "module"
                                }
                                +"React not loaded"
                            }
                        }
                    }
                }
            }
        }
    }
}