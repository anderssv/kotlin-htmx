package no.mikill.kotlin_htmx.validation

sealed class ValidationResult<T> {
    data class Valid<T>(
        val value: T,
    ) : ValidationResult<T>()

    data class Invalid<T>(
        val violations: Map<String, List<String>>,
    ) : ValidationResult<T>()
}
