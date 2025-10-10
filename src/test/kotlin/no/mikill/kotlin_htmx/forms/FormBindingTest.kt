package no.mikill.kotlin_htmx.forms

import io.ktor.http.Parameters
import io.ktor.http.parametersOf
import no.mikill.kotlin_htmx.registration.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FormBindingTest {
    @Test
    fun `should bind form parameters to Person object`() {
        // Arrange
        val parameters =
            parametersOf(
                "firstName" to listOf("John"),
                "lastName" to listOf("Doe"),
                "email" to listOf("john.doe@example.com"),
            )

        // Act
        val person = parameters.bindTo<Person>()

        // Assert
        assertThat(person.firstName).isEqualTo("John")
        assertThat(person.lastName).isEqualTo("Doe")
        assertThat(person.email).isEqualTo("john.doe@example.com")
        assertThat(person.addresses).isEmpty()
    }

    @Test
    fun `should handle empty string parameters`() {
        // Arrange
        val parameters =
            parametersOf(
                "firstName" to listOf("John"),
                "lastName" to listOf(""),
                "email" to listOf(""),
            )

        // Act
        val person = parameters.bindTo<Person>()

        // Assert
        assertThat(person.firstName).isEqualTo("John")
        assertThat(person.lastName).isEqualTo("")
        assertThat(person.email).isEqualTo("")
    }

    @Test
    fun `should handle nested properties with dot notation`() {
        // Arrange
        data class Address(
            val street: String,
            val city: String,
        )

        data class PersonWithAddress(
            val name: String,
            val address: Address,
        )

        val parameters =
            parametersOf(
                "name" to listOf("John"),
                "address.street" to listOf("123 Main St"),
                "address.city" to listOf("Springfield"),
            )

        // Act
        val person = parameters.bindTo<PersonWithAddress>()

        // Assert
        assertThat(person.name).isEqualTo("John")
        assertThat(person.address.street).isEqualTo("123 Main St")
        assertThat(person.address.city).isEqualTo("Springfield")
    }

    @Test
    fun `should extract and bind indexed property to Address object`() {
        // Arrange
        val parameters =
            parametersOf(
                "addresses[0].type" to listOf("HOME"),
                "addresses[0].streetAddress" to listOf("123 Main St"),
                "addresses[0].city" to listOf("Springfield"),
                "addresses[0].postalCode" to listOf("12345"),
                "addresses[0].country" to listOf("USA"),
            )

        // Act
        val address = parameters.bindIndexedProperty<no.mikill.kotlin_htmx.registration.Address>("addresses", 0)

        // Assert
        assertThat(address.streetAddress).isEqualTo("123 Main St")
        assertThat(address.city).isEqualTo("Springfield")
        assertThat(address.postalCode).isEqualTo("12345")
        assertThat(address.country).isEqualTo("USA")
    }
}
