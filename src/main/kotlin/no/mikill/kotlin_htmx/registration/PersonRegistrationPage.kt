package no.mikill.kotlin_htmx.registration

import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.submitInput
import no.mikill.kotlin_htmx.pages.validatedInputWithErrors
import no.mikill.kotlin_htmx.validation.toPath

class PersonRegistrationPage {
    fun renderPersonFormContent(
        container: FlowContent,
        person: Person,
        violations: Map<String, List<String>>,
    ) {
        container.apply {
            h1 { +"Register Person" }
            form(method = FormMethod.post, action = "/person/register") {
                div {
                    validatedInputWithErrors(
                        propertyPath = Person::firstName.toPath(),
                        value = person.firstName,
                        violations = violations,
                        label = "First Name",
                    )
                }
                div {
                    validatedInputWithErrors(
                        propertyPath = Person::lastName.toPath(),
                        value = person.lastName,
                        violations = violations,
                        label = "Last Name",
                    )
                }
                div {
                    validatedInputWithErrors(
                        propertyPath = Person::email.toPath(),
                        value = person.email,
                        violations = violations,
                        label = "Email",
                    )
                }
                submitInput { value = "Continue to Addresses" }
            }
        }
    }
}
