package no.mikill.kotlin_htmx.pages

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import no.mikill.kotlin_htmx.registration.Address
import no.mikill.kotlin_htmx.registration.AddressType
import no.mikill.kotlin_htmx.registration.Person
import no.mikill.kotlin_htmx.validation.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FormBuilderDslTest {
    @Test
    fun `should render simple field using DSL`() {
        // Arrange
        val person = Person(firstName = "John", lastName = "Doe", email = "john@example.com")
        val violations = emptyMap<String, List<String>>()

        // Act
        val html =
            createHTML().div {
                form(person, violations) {
                    field(Person::firstName.toPath(), "First Name")
                }
            }

        // Assert
        assertThat(html).contains("name=\"firstName\"")
        assertThat(html).contains("value=\"John\"")
        assertThat(html).contains("First Name")
    }

    @Test
    fun `should render multiple fields using DSL`() {
        // Arrange
        val person = Person(firstName = "John", lastName = "Doe", email = "john@example.com")
        val violations = emptyMap<String, List<String>>()

        // Act
        val html =
            createHTML().div {
                form(person, violations) {
                    field(Person::firstName.toPath(), "First Name")
                    field(Person::lastName.toPath(), "Last Name")
                    field(Person::email.toPath(), "Email")
                }
            }

        // Assert
        assertThat(html).contains("name=\"firstName\"")
        assertThat(html).contains("value=\"John\"")
        assertThat(html).contains("name=\"lastName\"")
        assertThat(html).contains("value=\"Doe\"")
        assertThat(html).contains("name=\"email\"")
        assertThat(html).contains("value=\"john@example.com\"")
    }

    @Test
    fun `should render violations using DSL`() {
        // Arrange
        val person = Person(firstName = "", lastName = "Doe", email = "john@example.com")
        val violations =
            mapOf(
                "firstName" to listOf("First name is required"),
            )

        // Act
        val html =
            createHTML().div {
                form(person, violations) {
                    field(Person::firstName.toPath(), "First Name")
                }
            }

        // Assert
        assertThat(html).contains("First name is required")
        assertThat(html).contains("form-error")
    }

    @Test
    fun `should render indexed fields using DSL`() {
        // Arrange
        val address =
            Address(
                type = AddressType.HOME,
                streetAddress = "123 Main St",
                city = "Springfield",
                postalCode = "12345",
                country = "USA",
            )
        val person = Person(firstName = "John", lastName = "Doe", email = "john@example.com", addresses = listOf(address))
        val violations = emptyMap<String, List<String>>()

        // Act
        val html =
            createHTML().div {
                indexedForm(person, violations, Person::addresses, 0) {
                    field(Address::streetAddress, "Street Address")
                    field(Address::city, "City")
                }
            }

        // Assert
        assertThat(html).contains("name=\"addresses[0].streetAddress\"")
        assertThat(html).contains("value=\"123 Main St\"")
        assertThat(html).contains("name=\"addresses[0].city\"")
        assertThat(html).contains("value=\"Springfield\"")
    }

    @Test
    fun `should render indexed field violations using DSL`() {
        // Arrange
        val address =
            Address(
                type = AddressType.HOME,
                streetAddress = "",
                city = "Springfield",
                postalCode = "12345",
                country = "USA",
            )
        val person = Person(firstName = "John", lastName = "Doe", email = "john@example.com", addresses = listOf(address))
        val violations =
            mapOf(
                "addresses[0].streetAddress" to listOf("Street address is required"),
            )

        // Act
        val html =
            createHTML().div {
                indexedForm(person, violations, Person::addresses, 0) {
                    field(Address::streetAddress, "Street Address")
                }
            }

        // Assert
        assertThat(html).contains("Street address is required")
        assertThat(html).contains("form-error")
    }
}
