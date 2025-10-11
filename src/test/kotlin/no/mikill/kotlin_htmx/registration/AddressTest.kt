package no.mikill.kotlin_htmx.registration

import jakarta.validation.Validation
import no.mikill.kotlin_htmx.validation.ValidationResult
import no.mikill.kotlin_htmx.validation.ValidationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AddressTest {
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val validator = validatorFactory.validator
    private val validationService = ValidationService(validator)

    @Test
    fun `Address validation should fail for invalid fields`() {
        // Arrange
        val invalidAddress =
            Address(
                type = null,
                streetAddress = "",
                city = "",
                postalCode = "invalid",
                country = "",
            )

        // Act
        val result = validationService.validate(invalidAddress)

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val violations = (result as ValidationResult.Invalid).violations
        assertThat(violations).containsKeys("type", "streetAddress", "city", "postalCode", "country")
    }

    @Test
    fun `Address validation should pass for valid address`() {
        // Arrange
        val validAddress =
            Address(
                type = AddressType.HOME,
                streetAddress = "123 Main St",
                city = "Springfield",
                postalCode = "12345",
                country = "USA",
            )

        // Act
        val result = validationService.validate(validAddress)

        // Assert
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
    }
}
