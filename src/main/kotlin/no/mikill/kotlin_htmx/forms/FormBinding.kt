package no.mikill.kotlin_htmx.forms

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.Parameters

/**
 * Binds HTTP form parameters to a typed object using Jackson.
 *
 * This provides type-safe form binding by converting form parameters to JSON
 * and then deserializing to the target type. Supports nested properties using
 * dot notation (e.g., "address.street") and indexed properties (e.g., "addresses[0].city").
 *
 * Example:
 * ```
 * val parameters = call.receiveParameters()
 * val person = parameters.bindTo<Person>()
 * ```
 */
inline fun <reified T : Any> Parameters.bindTo(): T {
    val objectMapper = createObjectMapper()
    val paramMap = toNestedMap()
    return objectMapper.convertValue(paramMap, T::class.java)
}

/**
 * Binds an indexed property from HTTP form parameters to a typed object.
 *
 * This extracts parameters with a specific indexed prefix (e.g., "addresses[0].city")
 * and binds them to the element type, stripping the index prefix.
 *
 * Example:
 * ```
 * // Given parameters: addresses[0].city=Boston, addresses[0].street=Main St
 * val address = parameters.bindIndexedProperty<Address>("addresses", 0)
 * // Returns Address(city="Boston", street="Main St")
 * ```
 *
 * @param propertyName The name of the indexed property (e.g., "addresses")
 * @param index The index to extract (e.g., 0)
 * @return The bound object of type T
 */
inline fun <reified T : Any> Parameters.bindIndexedProperty(
    propertyName: String,
    index: Int,
): T {
    val objectMapper = createObjectMapper()
    val prefix = "$propertyName[$index]."

    // Filter and transform parameters to remove the indexed prefix
    val filteredParams = mutableMapOf<String, String>()
    entries().forEach { (key, values) ->
        if (key.startsWith(prefix)) {
            val strippedKey = key.removePrefix(prefix)
            filteredParams[strippedKey] = values.firstOrNull() ?: ""
        }
    }

    return objectMapper.convertValue(filteredParams, T::class.java)
}

/**
 * Converts form parameters to a nested map structure.
 *
 * Handles:
 * - Simple properties: "firstName" -> {"firstName": "John"}
 * - Nested properties: "address.street" -> {"address": {"street": "123 Main St"}}
 * - Indexed properties: "addresses[0].city" -> {"addresses": [{"city": "Boston"}]}
 */
fun Parameters.toNestedMap(): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()

    entries().forEach { (key, values) ->
        val value = values.firstOrNull() ?: ""
        setNestedValue(result, key, value)
    }

    return result
}

/**
 * Sets a value in a nested map structure, creating intermediate maps/lists as needed.
 */
@Suppress("UNCHECKED_CAST")
private fun setNestedValue(
    map: MutableMap<String, Any?>,
    key: String,
    value: String,
) {
    val parts = parseKey(key)

    var current: Any = map
    for (i in 0 until parts.size - 1) {
        val part = parts[i]
        val nextPart = parts[i + 1]

        current =
            when {
                part is KeyPart.Property && nextPart is KeyPart.Property -> {
                    val currentMap = current as MutableMap<String, Any?>
                    currentMap.getOrPut(part.name) { mutableMapOf<String, Any?>() } as Any
                }

                part is KeyPart.Property && nextPart is KeyPart.Index -> {
                    val currentMap = current as MutableMap<String, Any?>
                    currentMap.getOrPut(part.name) { mutableListOf<MutableMap<String, Any?>>() } as Any
                }

                part is KeyPart.Index -> {
                    val currentList = current as MutableList<MutableMap<String, Any?>>
                    // Ensure list has enough elements
                    while (currentList.size <= part.index) {
                        currentList.add(mutableMapOf())
                    }
                    currentList[part.index] as Any
                }

                else -> {
                    current
                }
            }
    }

    // Set the final value
    val lastPart = parts.last()
    when {
        lastPart is KeyPart.Property && current is MutableMap<*, *> -> {
            (current as MutableMap<String, Any?>)[lastPart.name] = value
        }
    }
}

/**
 * Parses a key into parts (properties and indexes).
 * Examples:
 * - "firstName" -> [Property("firstName")]
 * - "address.street" -> [Property("address"), Property("street")]
 * - "addresses[0].city" -> [Property("addresses"), Index(0), Property("city")]
 */
private fun parseKey(key: String): List<KeyPart> {
    val parts = mutableListOf<KeyPart>()
    val regex = """(\w+)|\[(\d+)]""".toRegex()

    regex.findAll(key).forEach { match ->
        when {
            match.groups[1] != null -> parts.add(KeyPart.Property(match.groups[1]!!.value))
            match.groups[2] != null -> parts.add(KeyPart.Index(match.groups[2]!!.value.toInt()))
        }
    }

    return parts
}

private sealed class KeyPart {
    data class Property(
        val name: String,
    ) : KeyPart()

    data class Index(
        val index: Int,
    ) : KeyPart()
}

fun createObjectMapper(): ObjectMapper =
    jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
