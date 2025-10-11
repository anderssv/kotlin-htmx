package no.mikill.kotlin_htmx.registration

import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.submitInput
import no.mikill.kotlin_htmx.validation.toPath
import no.mikill.kotlin_htmx.pages.form as formDsl

class PersonRegistrationPage {
    fun renderPersonFormContent(
        container: FlowContent,
        person: Person,
        violations: Map<String, List<String>>,
    ) {
        container.apply {
            h1 { +"Register Person" }
            form(method = FormMethod.post, action = "/person/register") {
                formDsl(person, violations) {
                    field(Person::firstName.toPath(), "First Name")
                    field(Person::lastName.toPath(), "Last Name")
                    field(Person::email.toPath(), "Email")
                }
                submitInput { value = "Continue to Addresses" }
            }
        }
    }
}
