package no.mikill.kotlin_htmx.registration

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import jakarta.validation.Validation
import no.mikill.kotlin_htmx.validation.ValidationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class AddAddressRoutesTest {
    @Test
    fun `POST person id address add with valid address adds address and redirects`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            val person = Person.valid() // No addresses by default
            repository.save(person)

            // Act
            val response =
                client.post("/person/${person.id}/address/add") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        "addresses[0].type=HOME&" +
                            "addresses[0].streetAddress=456 Elm St&" +
                            "addresses[0].city=Portland&" +
                            "addresses[0].postalCode=97201&" +
                            "addresses[0].country=USA",
                    )
                }

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.Found)
            assertThat(response.headers["Location"]).isEqualTo("/person/${person.id}/address/add")

            // Verify address was added
            val updatedPerson = repository.findById(person.id)
            assertThat(updatedPerson).isNotNull()
            assertThat(updatedPerson!!.addresses).hasSize(1)
            assertThat(updatedPerson.addresses[0].streetAddress).isEqualTo("456 Elm St")
            assertThat(updatedPerson.addresses[0].city).isEqualTo("Portland")
        }

    @Test
    fun `POST person id address add with invalid address shows violations`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            val person = Person.valid() // No addresses by default
            repository.save(person)

            // Act - Invalid postal code and empty fields
            val response =
                client.post("/person/${person.id}/address/add") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        "addresses[0].type=HOME&" +
                            "addresses[0].streetAddress=&" +
                            "addresses[0].city=&" +
                            "addresses[0].postalCode=invalid&" +
                            "addresses[0].country=",
                    )
                }

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val html = response.bodyAsText()
            assertThat(html).contains("Add Address for")
            assertThat(html).contains("form-error")

            // Verify address was NOT added
            val updatedPerson = repository.findById(person.id)
            assertThat(updatedPerson!!.addresses).isEmpty()
        }

    @Test
    fun `POST person personId address addressId update with valid data updates existing address and preserves UUID`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            // Create person with existing address (with UUID)
            val existingAddressId = UUID.randomUUID()
            val existingAddress =
                Address(
                    id = existingAddressId,
                    type = AddressType.HOME,
                    streetAddress = "123 Main St",
                    city = "Springfield",
                    postalCode = "12345",
                    country = "USA",
                )
            val person = Person.valid().copy(addresses = listOf(existingAddress))
            repository.save(person)

            // Act - Update the address with modified data via the UUID-based endpoint
            val response =
                client.post("/person/${person.id}/address/$existingAddressId/update") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        "addressId=$existingAddressId&" +
                            "addresses[0].type=WORK&" + // Changed from HOME to WORK
                            "addresses[0].streetAddress=456 Office Blvd&" + // Changed
                            "addresses[0].city=Metropolis&" + // Changed
                            "addresses[0].postalCode=54321&" + // Changed
                            "addresses[0].country=Canada", // Changed
                    )
                }

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.Found)
            assertThat(response.headers["Location"]).isEqualTo("/person/${person.id}/address/add")

            // Verify address was updated and UUID preserved
            val updatedPerson = repository.findById(person.id)
            assertThat(updatedPerson).isNotNull()
            assertThat(updatedPerson!!.addresses).hasSize(1)

            val updatedAddress = updatedPerson.addresses[0]
            assertThat(updatedAddress.id).isEqualTo(existingAddressId) // UUID preserved
            assertThat(updatedAddress.type).isEqualTo(AddressType.WORK)
            assertThat(updatedAddress.streetAddress).isEqualTo("456 Office Blvd")
            assertThat(updatedAddress.city).isEqualTo("Metropolis")
            assertThat(updatedAddress.postalCode).isEqualTo("54321")
            assertThat(updatedAddress.country).isEqualTo("Canada")
        }

    @Test
    fun `POST person personId address addressId update with invalid data shows validation errors for edited address`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            // Create person with existing address (with UUID)
            val existingAddressId = UUID.randomUUID()
            val existingAddress =
                Address(
                    id = existingAddressId,
                    type = AddressType.HOME,
                    streetAddress = "123 Main St",
                    city = "Springfield",
                    postalCode = "12345",
                    country = "USA",
                )
            val person = Person.valid().copy(addresses = listOf(existingAddress))
            repository.save(person)

            // Act - Update the address with INVALID data via UUID-based endpoint
            val response =
                client.post("/person/${person.id}/address/$existingAddressId/update") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        "addressId=$existingAddressId&" +
                            "addresses[0].type=WORK&" +
                            "addresses[0].streetAddress=&" + // Invalid: empty
                            "addresses[0].city=&" + // Invalid: empty
                            "addresses[0].postalCode=invalid&" + // Invalid: wrong format
                            "addresses[0].country=", // Invalid: empty
                    )
                }

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.OK) // Shows form with errors
            val html = response.bodyAsText()
            assertThat(html).contains("Edit Address for") // On edit page
            assertThat(html).contains("form-error") // Validation errors present

            // Verify validation errors are shown for the correct address
            assertThat(html).contains("addresses[0].streetAddress") // Field path in error
            assertThat(html).contains("addresses[0].city")
            assertThat(html).contains("addresses[0].postalCode")
            assertThat(html).contains("addresses[0].country")

            // Verify address was NOT updated in repository
            val unchangedPerson = repository.findById(person.id)
            assertThat(unchangedPerson).isNotNull()
            assertThat(unchangedPerson!!.addresses).hasSize(1)
            assertThat(unchangedPerson.addresses[0].id).isEqualTo(existingAddressId)
            assertThat(unchangedPerson.addresses[0].type).isEqualTo(AddressType.HOME) // Unchanged
            assertThat(unchangedPerson.addresses[0].streetAddress).isEqualTo("123 Main St") // Unchanged
        }

    @Test
    fun `GET person personId address addressId edit displays edit form for specific address`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            // Create person with existing address (with UUID)
            val existingAddressId = UUID.randomUUID()
            val existingAddress =
                Address(
                    id = existingAddressId,
                    type = AddressType.HOME,
                    streetAddress = "123 Main St",
                    city = "Springfield",
                    postalCode = "12345",
                    country = "USA",
                )
            val person = Person.valid().copy(addresses = listOf(existingAddress))
            repository.save(person)

            // Act - GET the edit page for this address
            val response = client.get("/person/${person.id}/address/$existingAddressId/edit")

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val html = response.bodyAsText()
            assertThat(html).contains("Edit Address for")
            assertThat(html).contains("123 Main St")
            assertThat(html).contains("Springfield")
            assertThat(html).contains("HOME")
            assertThat(html).contains("action=\"/person/${person.id}/address/$existingAddressId/update\"")
        }
}
