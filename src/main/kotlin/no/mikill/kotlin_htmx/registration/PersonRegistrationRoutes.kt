package no.mikill.kotlin_htmx.registration

import io.ktor.server.application.Application
import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.mikill.kotlin_htmx.forms.bindIndexedProperty
import no.mikill.kotlin_htmx.forms.bindTo
import no.mikill.kotlin_htmx.pages.EmptyTemplate
import no.mikill.kotlin_htmx.pages.MainTemplate
import no.mikill.kotlin_htmx.routing.getUUID
import no.mikill.kotlin_htmx.validation.ValidationResult
import no.mikill.kotlin_htmx.validation.ValidationService

fun Application.configurePersonRegistrationRoutes(
    repository: PersonRepository,
    validationService: ValidationService,
) {
    routing {
        get("/person/register") {
            val person = Person(firstName = "", lastName = "", email = "", addresses = emptyList())
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Register Person")) {
                mainSectionTemplate {
                    emptyContentWrapper {
                        PersonRegistrationPage().renderPersonFormContent(this, person, emptyMap())
                    }
                }
            }
        }

        post("/person/register") {
            val parameters = call.receiveParameters()
            val person = parameters.bindTo<Person>()

            when (val result = validationService.validate(person)) {
                is ValidationResult.Valid -> {
                    repository.save(result.value)
                    call.respondRedirect("/person/${result.value.id}/address/add")
                }

                is ValidationResult.Invalid -> {
                    call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Register Person")) {
                        mainSectionTemplate {
                            emptyContentWrapper {
                                PersonRegistrationPage().renderPersonFormContent(this, person, result.violations)
                            }
                        }
                    }
                }
            }
        }

        get("/person/{id}/address/add") {
            val personId = call.parameters.getUUID("id")
            val person = repository.findById(personId) ?: return@get call.respondRedirect("/person/register")

            val emptyAddress = Address(type = null, streetAddress = "", city = "", postalCode = "", country = "")
            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Add Address")) {
                mainSectionTemplate {
                    emptyContentWrapper {
                        AddAddressPage().renderAddAddressFormContent(this, person, emptyAddress, emptyMap())
                    }
                }
            }
        }

        post("/person/{id}/address/add") {
            val personId = call.parameters.getUUID("id")
            val person = repository.findById(personId) ?: return@post call.respondRedirect("/person/register")

            val parameters = call.receiveParameters()
            val nextIndex = person.addresses.size

            // Parse address from form parameters using type-safe binding
            val newAddress = parameters.bindIndexedProperty<Address>("addresses", nextIndex)

            // Validate the new address
            when (val result = validationService.validate(newAddress)) {
                is ValidationResult.Valid -> {
                    val updatedPerson = person.copy(addresses = person.addresses + result.value)
                    repository.save(updatedPerson)
                    call.respondRedirect("/person/$personId/address/add")
                }

                is ValidationResult.Invalid -> {
                    // Remap violations to include the addresses[index] prefix
                    val remappedViolations =
                        result.violations.mapKeys { (key, _) ->
                            "addresses[$nextIndex].$key"
                        }
                    call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Add Address")) {
                        mainSectionTemplate {
                            emptyContentWrapper {
                                AddAddressPage().renderAddAddressFormContent(this, person, newAddress, remappedViolations)
                            }
                        }
                    }
                }
            }
        }

        get("/person/{personId}/address/{addressId}/edit") {
            val personId = call.parameters.getUUID("personId")
            val addressId = call.parameters.getUUID("addressId")
            val person = repository.findById(personId) ?: return@get call.respondRedirect("/person/register")

            // Find the address index by UUID
            val addressIndex = person.addresses.indexOfFirst { it.id == addressId }
            if (addressIndex == -1) {
                return@get call.respondRedirect("/person/$personId/address/add")
            }

            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Edit Address")) {
                mainSectionTemplate {
                    emptyContentWrapper {
                        EditAddressPage().renderEditAddressFormContent(this, person, addressIndex, emptyMap())
                    }
                }
            }
        }

        post("/person/{personId}/address/{addressId}/update") {
            val personId = call.parameters.getUUID("personId")
            val addressId = call.parameters.getUUID("addressId")
            val person = repository.findById(personId) ?: return@post call.respondRedirect("/person/register")

            // Find the address index by UUID
            val addressIndex = person.addresses.indexOfFirst { it.id == addressId }
            if (addressIndex == -1) {
                return@post call.respondRedirect("/person/$personId/address/add")
            }

            val parameters = call.receiveParameters()

            // Parse the updated address from form parameters
            val updatedAddress = parameters.bindIndexedProperty<Address>("addresses", addressIndex)

            // Create a new list of addresses with the updated one
            val updatedAddresses =
                person.addresses.toMutableList().apply {
                    this[addressIndex] = updatedAddress.copy(id = addressId) // Preserve the UUID
                }

            // Create person with updated addresses for validation
            val personWithUpdatedAddress = person.copy(addresses = updatedAddresses)

            // Validate the updated address
            when (val result = validationService.validate(personWithUpdatedAddress)) {
                is ValidationResult.Valid -> {
                    repository.save(result.value)
                    call.respondRedirect("/person/$personId/address/add")
                }

                is ValidationResult.Invalid -> {
                    // Show the form again with validation errors
                    call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Edit Address")) {
                        mainSectionTemplate {
                            emptyContentWrapper {
                                EditAddressPage().renderEditAddressFormContent(
                                    this,
                                    personWithUpdatedAddress,
                                    addressIndex,
                                    result.violations,
                                )
                            }
                        }
                    }
                }
            }
        }

        post("/person/{id}/complete") {
            val personId = call.parameters.getUUID("id")
            val person = repository.findById(personId) ?: return@post call.respondRedirect("/person/register")

            // Validate that person has at least one address
            if (person.addresses.isEmpty()) {
                val violations = mapOf("addresses" to listOf("At least one address is required"))
                call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Add Address")) {
                    mainSectionTemplate {
                        emptyContentWrapper {
                            val emptyAddress =
                                Address(type = null, streetAddress = "", city = "", postalCode = "", country = "")
                            AddAddressPage().renderAddAddressFormContent(this, person, emptyAddress, violations)
                        }
                    }
                }
            } else {
                call.respondRedirect("/person/$personId")
            }
        }

        get("/person/{id}") {
            val personId = call.parameters.getUUID("id")
            val person = repository.findById(personId) ?: return@get call.respondRedirect("/person/register")

            call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Person Details")) {
                mainSectionTemplate {
                    emptyContentWrapper {
                        ViewPersonPage().renderPersonDetails(this, person)
                    }
                }
            }
        }
    }
}
