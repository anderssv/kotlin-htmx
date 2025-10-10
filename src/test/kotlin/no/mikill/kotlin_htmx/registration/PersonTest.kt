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
    fun `Person should have id firstName lastName email addresses fields with Valid annotation on addresses`() {
        // Arrange & Act
        val person =
            Person(
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                addresses =
                    listOf(
                        Address(
                            type = AddressType.HOME,
                            streetAddress = "123 Main St",
                            city = "Springfield",
                            postalCode = "12345",
                            country = "USA",
                        ),
                    ),
            )

        // Assert
        assertThat(person.id).isNotNull()
        assertThat(person.firstName).isEqualTo("John")
        assertThat(person.lastName).isEqualTo("Doe")
        assertThat(person.email).isEqualTo("john.doe@example.com")
        assertThat(person.addresses).hasSize(1)
    }

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

    @Test
    fun `Person valid test data builder creates valid person`() {
        // Arrange & Act
        val person = Person.valid()

        // Assert
        val result = validationService.validate(person)
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat(person.firstName).isEqualTo("John")
        assertThat(person.lastName).isEqualTo("Doe")
        assertThat(person.email).isEqualTo("john.doe@example.com")
        assertThat(person.addresses).hasSize(1)
    }

    @Test
    fun `Address valid test data builder creates valid address`() {
        // Arrange & Act
        val address = Address.valid()

        // Assert
        val result = validationService.validate(address)
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat(address.type).isEqualTo(AddressType.HOME)
        assertThat(address.streetAddress).isEqualTo("123 Main St")
        assertThat(address.city).isEqualTo("Springfield")
        assertThat(address.postalCode).isEqualTo("12345")
        assertThat(address.country).isEqualTo("USA")
    }
}
