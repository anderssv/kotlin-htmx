package no.mikill.kotlin_htmx.pages

import kotlinx.html.InputType
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import no.mikill.kotlin_htmx.registration.Address
import no.mikill.kotlin_htmx.registration.Person
import no.mikill.kotlin_htmx.validation.at
import no.mikill.kotlin_htmx.validation.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PropertyPathFormComponentsTest {
    @Test
    fun `validatedInput accepts PropertyPath and renders input with path as name`() {
        // Arrange & Act
        val html =
            createHTML().div {
                validatedInput(
                    propertyPath = Person::firstName.toPath(),
                    value = "John",
                    label = "First Name",
                    inputType = InputType.text,
                    placeholder = "Enter first name",
                )
            }

        // Assert
        assertThat(html).contains("name=\"firstName\"")
        assertThat(html).contains("value=\"John\"")
        assertThat(html).contains("placeholder=\"Enter first name\"")
        assertThat(html).contains("First Name")
    }

    @Test
    fun `validatedInput extracts HTML constraints from PropertyPath annotations`() {
        // Arrange & Act
        val html =
            createHTML().div {
                validatedInput(
                    propertyPath = Person::firstName.toPath(),
                    value = "John",
                    label = "First Name",
                )
            }

        // Assert - Person::firstName has @NotBlank and @Size(max=50)
        assertThat(html).contains("required")
        assertThat(html).contains("maxlength=\"50\"")
    }

    @Test
    fun `validatedInputWithErrors displays violations by path`() {
        // Arrange
        val violations =
            mapOf(
                "firstName" to listOf("First name is required", "First name must be 50 characters or less"),
            )

        // Act
        val html =
            createHTML().div {
                validatedInputWithErrors(
                    propertyPath = Person::firstName.toPath(),
                    value = "",
                    violations = violations,
                    label = "First Name",
                )
            }

        // Assert
        assertThat(html).contains("First name is required")
        assertThat(html).contains("First name must be 50 characters or less")
        assertThat(html).contains("error-message")
    }

    @Test
    fun `validatedInput works with nested indexed PropertyPath`() {
        // Arrange & Act - Use Person::addresses.at(0, Address::city)
        val html =
            createHTML().div {
                validatedInput(
                    propertyPath = Person::addresses.at(0, Address::city),
                    value = "Springfield",
                    label = "City",
                )
            }

        // Assert
        assertThat(html).contains("name=\"addresses[0].city\"")
        assertThat(html).contains("value=\"Springfield\"")
        // Address::city has @NotBlank and @Size(max=50)
        assertThat(html).contains("required")
        assertThat(html).contains("maxlength=\"50\"")
    }
}
