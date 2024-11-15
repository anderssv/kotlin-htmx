package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.HtmlElements.htmlTodolistSectionContent
import no.mikill.kotlin_htmx.pages.HtmlElements.htmxTodolistSectionContent
import no.mikill.kotlin_htmx.pages.Styles.BOX_STYLE
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds

class MultiDemoPage {

    suspend fun renderMultiJsPage(context: RoutingContext) {
        with(context) {
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Multi frameworks demo")) {
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
                mainSectionTemplate {
                    emptyContentWrapper {
                        htmlTodolistSectionContent()
                        htmxTodolistSectionContent(loadDelay = 5.seconds, backendDelay = 0.seconds)
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