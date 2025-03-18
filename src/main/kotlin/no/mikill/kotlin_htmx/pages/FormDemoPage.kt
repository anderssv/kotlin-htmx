package no.mikill.kotlin_htmx.pages

import io.ktor.server.html.*
import io.ktor.server.routing.*
import jakarta.validation.ConstraintViolation
import kotlinx.html.*
import no.mikill.kotlin_htmx.application.Application
import no.mikill.kotlin_htmx.pages.FormUtils.inputFieldWithValidationAndErrors
import kotlin.collections.Set
import kotlin.collections.set

class FormDemoPage {

    suspend fun renderInputForm(
        context: RoutingContext,
        existingApplication: Application?,
        errors: Set<ConstraintViolation<Application>>
    ) {
        context.call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Form input")) {
            headerContent {
                span { +"Form demo" }
            }
            mainSectionTemplate {
                emptyContentWrapper {
                    // This is a custom script that I have written. It should be packaged as a library and published
                    // to NPM instead of just using a direct link to Github. Let me know if you want to use it and
                    // I will add it to the project.
                    script { src = "https://cdn.jsdelivr.net/gh/anderssv/formjson/src/formjson.js" }
                    form {
                        attributes["formjson"] = "true"
                        method = FormMethod.post

                        inputFieldWithValidationAndErrors(
                            existingApplication,
                            "person.firstName",
                            "First name",
                            errors
                        )
                        inputFieldWithValidationAndErrors(
                            existingApplication,
                            "person.lastName",
                            "Last name",
                            errors
                        )
                        submitInput { name = "ok" }
                    }
                }
            }
        }
    }

    suspend fun renderFormSaved(
        context: RoutingContext,
        existingApplication: Application
    ) {
        context.call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Form saved")) {
            mainSectionTemplate {
                emptyContentWrapper {
                    section {
                        h1 { +"Form save" }
                        div {
                            +"Form saved"
                        }
                        div {
                            +"Application object data: "
                            dl {
                                dt { +"First name" }
                                dd { +existingApplication.person.firstName }
                                dt { +"Last name" }
                                dd { +existingApplication.person.lastName }
                                dt { +"Comments" }
                                dd { +existingApplication.comments }
                            }
                        }
                    }
                }
            }
        }
    }
}