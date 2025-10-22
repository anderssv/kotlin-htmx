package no.mikill.kotlin_htmx.routing

import io.ktor.http.Parameters
import java.util.UUID

fun Parameters.getUUID(name: String): UUID {
    val value =
        this[name]
            ?: throw IllegalArgumentException("Parameter $name is required")
    return try {
        UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid UUID in parameter: $name", e)
    }
}
