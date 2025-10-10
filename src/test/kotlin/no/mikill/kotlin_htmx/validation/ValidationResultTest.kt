package no.mikill.kotlin_htmx.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ValidationResultTest {
    @Test
    fun `Valid should hold a value`() {
        val value = "test string"
        val result = ValidationResult.Valid(value)

        assertThat(result.value).isEqualTo(value)
    }

    @Test
    fun `Invalid should hold a map of property paths to error messages`() {
        val violations =
            mapOf(
                "name" to listOf("must not be blank"),
                "email" to listOf("must be a valid email", "must not be blank"),
            )
        val result = ValidationResult.Invalid<String>(violations)

        assertThat(result.violations).isEqualTo(violations)
    }

    @Test
    fun `should be able to use when expression exhaustively`() {
        val validResult: ValidationResult<String> = ValidationResult.Valid("test")
        val invalidResult: ValidationResult<String> = ValidationResult.Invalid(mapOf("field" to listOf("error")))

        val validMessage =
            when (validResult) {
                is ValidationResult.Valid -> "Got value: ${validResult.value}"
                is ValidationResult.Invalid -> "Got errors: ${validResult.violations}"
            }

        val invalidMessage =
            when (invalidResult) {
                is ValidationResult.Valid -> "Got value: ${invalidResult.value}"
                is ValidationResult.Invalid -> "Got errors: ${invalidResult.violations}"
            }

        assertThat(validMessage).isEqualTo("Got value: test")
        assertThat(invalidMessage).contains("Got errors:")
    }
}
