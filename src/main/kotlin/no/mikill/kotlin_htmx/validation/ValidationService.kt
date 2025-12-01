package no.mikill.kotlin_htmx.validation

import jakarta.validation.Validator

class ValidationService(
    private val validator: Validator,
) {
    fun <T> validate(value: T): ValidationResult<T> {
        val violations = validator.validate(value)
        return when {
            violations.isEmpty() -> {
                ValidationResult.Valid(value)
            }

            else -> {
                val violationMap =
                    violations.groupBy(
                        { it.propertyPath.toString() },
                        { it.message },
                    )
                ValidationResult.Invalid(violationMap)
            }
        }
    }
}
