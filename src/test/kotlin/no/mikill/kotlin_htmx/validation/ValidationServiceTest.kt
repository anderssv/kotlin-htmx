package no.mikill.kotlin_htmx.validation

import jakarta.validation.Valid
import jakarta.validation.Validation
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ValidationServiceTest {
    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val validator = validatorFactory.validator
    private val validationService = ValidationService(validator)

    data class TestPerson(
        @field:NotBlank
        val name: String,
        @field:Size(min = 3, max = 100)
        val email: String,
    )

    @Test
    fun `should return Valid when object has no constraint violations`() {
        val validPerson = TestPerson(name = "John", email = "john@example.com")

        val result = validationService.validate(validPerson)

        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        val validResult = result as ValidationResult.Valid
        assertThat(validResult.value).isEqualTo(validPerson)
    }

    @Test
    fun `should return Invalid with grouped violations when object has constraint violations`() {
        val invalidPerson = TestPerson(name = "", email = "ab")

        val result = validationService.validate(invalidPerson)

        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.violations).containsKey("name")
        assertThat(invalidResult.violations).containsKey("email")
        assertThat(invalidResult.violations["name"]).hasSize(1)
        assertThat(invalidResult.violations["email"]).hasSize(1)
    }

    data class TestAccount(
        @field:NotBlank
        @field:Size(min = 5, max = 20)
        val username: String,
    )

    @Test
    fun `should handle multiple violations per field`() {
        val invalidAccount = TestAccount(username = "ab")

        val result = validationService.validate(invalidAccount)

        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.violations).containsKey("username")
        assertThat(invalidResult.violations["username"]).hasSizeGreaterThanOrEqualTo(1)
    }

    data class Address(
        @field:NotBlank
        val street: String,
        @field:NotBlank
        val city: String,
    )

    data class PersonWithAddress(
        @field:NotBlank
        val name: String,
        @field:Valid
        val address: Address,
    )

    @Test
    fun `should cascade validation to nested objects`() {
        val invalidPerson =
            PersonWithAddress(
                name = "",
                address = Address(street = "", city = ""),
            )

        val result = validationService.validate(invalidPerson)

        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        val invalidResult = result as ValidationResult.Invalid
        assertThat(invalidResult.violations).containsKey("name")
        assertThat(invalidResult.violations).containsKey("address.street")
        assertThat(invalidResult.violations).containsKey("address.city")
    }
}
