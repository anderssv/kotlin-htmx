package no.mikill.kotlin_htmx.registration

import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.hiddenInput
import kotlinx.html.submitInput
import no.mikill.kotlin_htmx.pages.indexedForm

class EditAddressPage {
    fun renderEditAddressFormContent(
        container: FlowContent,
        person: Person,
        addressIndex: Int,
        violations: Map<String, List<String>>,
    ) {
        container.apply {
            h1 { +"Edit Address for ${person.firstName} ${person.lastName}" }

            val address = person.addresses[addressIndex]

            // Show general violations
            violations["addresses"]?.forEach { error ->
                div(classes = "form-error") {
                    +error
                }
            }

            form(method = FormMethod.post, action = "/person/${person.id}/address/${address.id}/update") {
                // Hidden field for address UUID
                hiddenInput {
                    name = "addressId"
                    value = address.id.toString()
                }

                indexedForm(person, violations, Person::addresses, addressIndex) {
                    enumSelect(Address::type, "Address Type")
                    field(Address::streetAddress, "Street Address")
                    field(Address::city, "City")
                    field(Address::postalCode, "Postal Code")
                    field(Address::country, "Country")
                }
                submitInput { value = "Update Address" }
            }
        }
    }
}
