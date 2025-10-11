package no.mikill.kotlin_htmx.registration

import jakarta.validation.Validation
import no.mikill.kotlin_htmx.validation.ValidationResult
import no.mikill.kotlin_htmx.validation.ValidationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonTest {
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val validator = validatorFactory.validator
    private val validationService = ValidationService(validator)

    @Test
    fun `Person validates nested Address violations with correct paths`() {
        // Arrange
        val personWithInvalidAddress =
            Person(
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                addresses =
                    listOf(
                        Address(
                            type = null,
                            streetAddress = "",
                            city = "",
                            postalCode = "invalid",
                            country = "",
                        ),
                    ),
            )

        // Act
        val result = validationService.validate(personWithInvalidAddress)

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val violations = (result as ValidationResult.Invalid).violations
        assertThat(violations).containsKeys(
            "addresses[0].type",
            "addresses[0].streetAddress",
            "addresses[0].city",
            "addresses[0].postalCode",
            "addresses[0].country",
        )
    }

    @Test
    fun `Person allows empty addresses list during registration`() {
        // Arrange
        val personWithNoAddresses =
            Person(
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                addresses = emptyList(),
            )

        // Act
        val result = validationService.validate(personWithNoAddresses)

        // Assert - Should be valid, addresses will be added in next step
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
    }
}
