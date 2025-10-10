package no.mikill.kotlin_htmx.registration

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.html.body
import kotlinx.html.div
import no.mikill.kotlin_htmx.validation.ValidationResult
import no.mikill.kotlin_htmx.validation.ValidationService
import java.util.UUID

fun Application.configurePersonRegistrationRoutes(
    repository: PersonRepository,
    validationService: ValidationService,
) {
    routing {
        get("/person/register") {
            val person = Person(firstName = "", lastName = "", email = "", addresses = emptyList())
            call.respondHtml(status = HttpStatusCode.OK) {
                body {
                    div {
                        PersonRegistrationPage().renderPersonFormContent(this, person, emptyMap())
                    }
                }
            }
        }

        post("/person/register") {
            val parameters = call.receiveParameters()
            val firstName = parameters["firstName"] ?: ""
            val lastName = parameters["lastName"] ?: ""
            val email = parameters["email"] ?: ""

            val person =
                Person(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    addresses = emptyList(),
                )

            when (val result = validationService.validate(person)) {
                is ValidationResult.Valid -> {
                    repository.save(result.value)
                    call.respondRedirect("/person/${result.value.id}/address/add")
                }
                is ValidationResult.Invalid -> {
                    call.respondHtml(status = HttpStatusCode.OK) {
                        body {
                            div {
                                PersonRegistrationPage().renderPersonFormContent(this, person, result.violations)
                            }
                        }
                    }
                }
            }
        }

        get("/person/{id}/address/add") {
            val personId = UUID.fromString(call.parameters["id"]!!)
            val person = repository.findById(personId) ?: return@get call.respondRedirect("/person/register")

            val emptyAddress = Address(type = null, streetAddress = "", city = "", postalCode = "", country = "")
            call.respondHtml(status = HttpStatusCode.OK) {
                body {
                    div {
                        AddAddressPage().renderAddAddressFormContent(this, person, emptyAddress, emptyMap())
                    }
                }
            }
        }

        post("/person/{id}/address/add") {
            val personId = UUID.fromString(call.parameters["id"]!!)
            val person = repository.findById(personId) ?: return@post call.respondRedirect("/person/register")

            val parameters = call.receiveParameters()
            val nextIndex = person.addresses.size

            // Parse address from form parameters
            val addressType =
                parameters["addresses[$nextIndex].type"]?.let {
                    AddressType.valueOf(it)
                }
            val streetAddress = parameters["addresses[$nextIndex].streetAddress"] ?: ""
            val city = parameters["addresses[$nextIndex].city"] ?: ""
            val postalCode = parameters["addresses[$nextIndex].postalCode"] ?: ""
            val country = parameters["addresses[$nextIndex].country"] ?: ""

            val newAddress =
                Address(
                    type = addressType,
                    streetAddress = streetAddress,
                    city = city,
                    postalCode = postalCode,
                    country = country,
                )

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
                    call.respondHtml(status = HttpStatusCode.OK) {
                        body {
                            div {
                                AddAddressPage().renderAddAddressFormContent(this, person, newAddress, remappedViolations)
                            }
                        }
                    }
                }
            }
        }

        post("/person/{id}/complete") {
            val personId = UUID.fromString(call.parameters["id"]!!)
            val person = repository.findById(personId) ?: return@post call.respondRedirect("/person/register")

            // Validate that person has at least one address
            if (person.addresses.isEmpty()) {
                val violations = mapOf("addresses" to listOf("At least one address is required"))
                call.respondHtml(status = HttpStatusCode.OK) {
                    body {
                        div {
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
            val personId = UUID.fromString(call.parameters["id"]!!)
            val person = repository.findById(personId) ?: return@get call.respondRedirect("/person/register")

            call.respondHtml(status = HttpStatusCode.OK) {
                body {
                    div {
                        ViewPersonPage().renderPersonDetails(this, person)
                    }
                }
            }
        }
    }
}
