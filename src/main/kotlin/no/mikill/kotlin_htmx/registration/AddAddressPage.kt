package no.mikill.kotlin_htmx.registration

import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
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
import no.mikill.kotlin_htmx.pages.validatedInputWithErrors
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
                            AddressType.values().forEach { type ->
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
                div {
                    validatedInputWithErrors(
                        propertyPath = Person::addresses.at(nextIndex, Address::streetAddress),
                        value = newAddress.streetAddress,
                        violations = violations,
                        label = "Street Address",
                    )
                }
                div {
                    validatedInputWithErrors(
                        propertyPath = Person::addresses.at(nextIndex, Address::city),
                        value = newAddress.city,
                        violations = violations,
                        label = "City",
                    )
                }
                div {
                    validatedInputWithErrors(
                        propertyPath = Person::addresses.at(nextIndex, Address::postalCode),
                        value = newAddress.postalCode,
                        violations = violations,
                        label = "Postal Code",
                    )
                }
                div {
                    validatedInputWithErrors(
                        propertyPath = Person::addresses.at(nextIndex, Address::country),
                        value = newAddress.country,
                        violations = violations,
                        label = "Country",
                    )
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
