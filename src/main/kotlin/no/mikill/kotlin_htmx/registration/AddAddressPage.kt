package no.mikill.kotlin_htmx.registration

import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.option
import kotlinx.html.select
import kotlinx.html.submitInput
import kotlinx.html.ul
import no.mikill.kotlin_htmx.pages.indexedForm
import no.mikill.kotlin_htmx.validation.at

class AddAddressPage {
    fun renderAddAddressFormContent(
        container: FlowContent,
        person: Person,
        newAddress: Address,
        violations: Map<String, List<String>>,
    ) {
        container.apply {
            val nextIndex = person.addresses.size
            // Create a temporary person with the new address at the correct index for value extraction
            val personWithNewAddress = person.copy(addresses = person.addresses + newAddress)

            h1 { +"Add Address for ${person.firstName} ${person.lastName}" }

            // Show general violations
            violations["addresses"]?.forEach { error ->
                div(classes = "form-error") {
                    +error
                }
            }

            if (person.addresses.isNotEmpty()) {
                h2 { +"Existing Addresses" }
                ul {
                    person.addresses.forEach { address ->
                        li {
                            +"${address.type}: ${address.streetAddress}, ${address.city}"
                        }
                    }
                }
            }

            h2 { +"New Address" }
            form(method = FormMethod.post, action = "/person/${person.id}/address/add") {
                div {
                    label {
                        +"Address Type"
                        select {
                            name = Person::addresses.at(nextIndex, Address::type).path
                            AddressType.entries.forEach { type ->
                                option {
                                    value = type.name
                                    if (type == newAddress.type) {
                                        selected = true
                                    }
                                    +type.name
                                }
                            }
                        }
                    }
                }
                indexedForm(personWithNewAddress, violations, Person::addresses, nextIndex) {
                    field(Address::streetAddress, "Street Address")
                    field(Address::city, "City")
                    field(Address::postalCode, "Postal Code")
                    field(Address::country, "Country")
                }
                submitInput { value = "Add Address" }
            }

            if (person.addresses.isNotEmpty()) {
                form(method = FormMethod.post, action = "/person/${person.id}/complete") {
                    submitInput { value = "Complete Registration" }
                }
            }
        }
    }
}
