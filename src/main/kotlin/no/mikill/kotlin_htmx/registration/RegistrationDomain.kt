package no.mikill.kotlin_htmx.registration

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class Person(
    val id: UUID = UUID.randomUUID(),
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 50, message = "First name must be 50 characters or less")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 50, message = "Last name must be 50 characters or less")
    val lastName: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Must be a valid email address")
    val email: String,
    @field:Valid
    @field:Size(min = 0, message = "Addresses list cannot be null")
    val addresses: List<Address> = emptyList(),
) {
    companion object
}

data class Address(
    val id: UUID = UUID.randomUUID(),
    @field:NotNull(message = "Address type is required")
    val type: AddressType?,
    @field:NotBlank(message = "Street address is required")
    @field:Size(max = 100, message = "Street address must be 100 characters or less")
    val streetAddress: String,
    @field:NotBlank(message = "City is required")
    @field:Size(max = 50, message = "City must be 50 characters or less")
    val city: String,
    @field:NotBlank(message = "Postal code is required")
    @field:Pattern(regexp = "[0-9]{4,5}", message = "Postal code must be 4-5 digits")
    val postalCode: String,
    @field:NotBlank(message = "Country is required")
    @field:Size(max = 50, message = "Country must be 50 characters or less")
    val country: String,
) {
    companion object
}

/**
 * Creates a valid Person with default values.
 * Use .copy() to override specific properties in tests.
 *
 * @param numberOfAddresses Controls the complexity of the object graph (default: 0)
 */
fun Person.Companion.valid(numberOfAddresses: Int = 0) =
    Person(
        firstName = "John",
        lastName = "Doe",
        email = "john.doe@example.com",
        addresses = (1..numberOfAddresses).map { Address.valid() },
    )

/**
 * Creates a valid Address with default values.
 * Use .copy() to override specific properties in tests.
 */
fun Address.Companion.valid() =
    Address(
        type = AddressType.HOME,
        streetAddress = "123 Main St",
        city = "Springfield",
        postalCode = "12345",
        country = "USA",
    )
