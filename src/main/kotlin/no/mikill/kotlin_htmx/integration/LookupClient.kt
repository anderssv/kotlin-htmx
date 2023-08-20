package no.mikill.kotlin_htmx.integration

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import no.mikill.kotlin_htmx.items

sealed class LookupResult {
    data class Success(val response: String) : LookupResult()
    data object NotFound : LookupResult()
    data class InvalidInput(val message: String) : LookupResult()
    data class Failure(val reason: String) : LookupResult()
}

class LookupClient(private val apiKey: String) {
    private val client = HttpClient(CIO)

    fun lookup(lookupValue: String): LookupResult {
        return when {
            lookupValue == "Invalid" -> LookupResult.InvalidInput("Invalid value")
            items.singleOrNull { it.name == lookupValue } != null -> LookupResult.Success(lookupValue)
            else -> LookupResult.NotFound
        }
    }
}