package no.mikill.kotlin_htmx.registration

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
}
