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

class PersonRegistrationRoutesTest {
    @Test
    fun `POST person register with valid data saves person and redirects to add address page`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            // Act
            val response =
                client.post("/person/register") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("firstName=John&lastName=Doe&email=john.doe@example.com")
                }

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.Found)
            assertThat(response.headers["Location"]).contains("/person/")
            assertThat(response.headers["Location"]).contains("/address/add")

            // Verify person was saved
            val persons = repository.findAll()
            assertThat(persons).hasSize(1)
            assertThat(persons[0].firstName).isEqualTo("John")
            assertThat(persons[0].lastName).isEqualTo("Doe")
            assertThat(persons[0].email).isEqualTo("john.doe@example.com")
            assertThat(persons[0].addresses).isEmpty()
        }

    @Test
    fun `POST person register with invalid data shows violations`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            // Act - Missing required fields
            val response =
                client.post("/person/register") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("firstName=&lastName=&email=invalid-email")
                }

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val html = response.bodyAsText()
            assertThat(html).contains("Register Person")
            assertThat(html).contains("error-message")

            // Verify no person was saved
            assertThat(repository.findAll()).isEmpty()
        }

    @Test
    fun `POST person id complete with addresses redirects to view person page`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            val person = Person.valid()
            repository.save(person)

            // Act
            val response = client.post("/person/${person.id}/complete")

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.Found)
            assertThat(response.headers["Location"]).isEqualTo("/person/${person.id}")
        }

    @Test
    fun `POST person id complete without addresses shows error`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            val person = Person.valid(addresses = emptyList())
            repository.save(person)

            // Act
            val response = client.post("/person/${person.id}/complete")

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val html = response.bodyAsText()
            assertThat(html).contains("At least one address is required")
        }

    @Test
    fun `GET person id displays complete person`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            val person = Person.valid()
            repository.save(person)

            // Act
            val response = client.get("/person/${person.id}")

            // Assert
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val html = response.bodyAsText()
            assertThat(html).contains("Person Details")
            assertThat(html).contains("John Doe")
            assertThat(html).contains("john.doe@example.com")
            assertThat(html).contains("123 Main St")
            assertThat(html).contains("Springfield")
        }

    @Test
    fun `complete person registration flow from initial form to final view`() =
        testApplication {
            // Arrange
            val repository = PersonRepository()
            val validatorFactory = Validation.buildDefaultValidatorFactory()
            val validationService = ValidationService(validatorFactory.validator)

            application {
                configurePersonRegistrationRoutes(repository, validationService)
            }

            // Act & Assert - Step 1: Submit person details
            val registerResponse =
                client.post("/person/register") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("firstName=Alice&lastName=Johnson&email=alice.johnson@example.com")
                }

            assertThat(registerResponse.status).isEqualTo(HttpStatusCode.Found)
            val addAddressUrl = registerResponse.headers["Location"]!!
            assertThat(addAddressUrl).matches("/person/[a-f0-9-]+/address/add")

            // Step 2: View add address page
            val addAddressPageResponse = client.get(addAddressUrl)
            assertThat(addAddressPageResponse.status).isEqualTo(HttpStatusCode.OK)
            val addAddressHtml = addAddressPageResponse.bodyAsText()
            assertThat(addAddressHtml).contains("Add Address for Alice Johnson")

            // Step 3: Submit first address (Home)
            val firstAddressResponse =
                client.post(addAddressUrl) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("addresses[0].type=HOME&addresses[0].streetAddress=123 Main St&addresses[0].city=Springfield&addresses[0].postalCode=12345&addresses[0].country=USA")
                }

            assertThat(firstAddressResponse.status).isEqualTo(HttpStatusCode.Found)
            assertThat(firstAddressResponse.headers["Location"]).isEqualTo(addAddressUrl)

            // Step 4: View add address page again (should show existing address)
            val addSecondAddressPageResponse = client.get(addAddressUrl)
            assertThat(addSecondAddressPageResponse.status).isEqualTo(HttpStatusCode.OK)
            val addSecondAddressHtml = addSecondAddressPageResponse.bodyAsText()
            assertThat(addSecondAddressHtml).contains("Existing Addresses")
            assertThat(addSecondAddressHtml).contains("HOME: 123 Main St, Springfield")

            // Step 5: Submit second address (Work)
            val secondAddressResponse =
                client.post(addAddressUrl) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("addresses[1].type=WORK&addresses[1].streetAddress=456 Office Blvd&addresses[1].city=Metropolis&addresses[1].postalCode=67890&addresses[1].country=USA")
                }

            assertThat(secondAddressResponse.status).isEqualTo(HttpStatusCode.Found)

            // Step 6: Complete registration
            val personId = addAddressUrl.substringAfter("/person/").substringBefore("/address/add")
            val completeResponse = client.post("/person/$personId/complete")

            assertThat(completeResponse.status).isEqualTo(HttpStatusCode.Found)
            assertThat(completeResponse.headers["Location"]).isEqualTo("/person/$personId")

            // Step 7: View final person details
            val viewPersonResponse = client.get("/person/$personId")
            assertThat(viewPersonResponse.status).isEqualTo(HttpStatusCode.OK)
            val viewPersonHtml = viewPersonResponse.bodyAsText()
            assertThat(viewPersonHtml).contains("Person Details")
            assertThat(viewPersonHtml).contains("Name: Alice Johnson")
            assertThat(viewPersonHtml).contains("Email: alice.johnson@example.com")
            assertThat(viewPersonHtml).contains("HOME: 123 Main St, Springfield, 12345, USA")
            assertThat(viewPersonHtml).contains("WORK: 456 Office Blvd, Metropolis, 67890, USA")

            // Verify final state in repository
            val savedPerson = repository.findAll().first()
            assertThat(savedPerson.firstName).isEqualTo("Alice")
            assertThat(savedPerson.lastName).isEqualTo("Johnson")
            assertThat(savedPerson.email).isEqualTo("alice.johnson@example.com")
            assertThat(savedPerson.addresses).hasSize(2)
            assertThat(savedPerson.addresses[0].type).isEqualTo(AddressType.HOME)
            assertThat(savedPerson.addresses[1].type).isEqualTo(AddressType.WORK)
        }
}
