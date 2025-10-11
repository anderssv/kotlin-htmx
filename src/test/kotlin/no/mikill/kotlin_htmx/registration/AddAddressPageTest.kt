package no.mikill.kotlin_htmx.registration

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class AddAddressPageTest {
    @Test
    fun `AddAddressPage renders address form with person context`() {
        // Arrange
        val page = AddAddressPage()
        val person = Person.valid(numberOfAddresses = 1) // Has 1 existing address
        val newAddress = Address(type = null, streetAddress = "", city = "", postalCode = "", country = "")

        // Act
        val html =
            createHTML().div {
                page.renderAddAddressFormContent(this, person, newAddress, emptyMap())
            }

        // Assert
        assertThat(html).contains("Add Address for ${person.firstName} ${person.lastName}")
        assertThat(html).contains("name=\"addresses[1].streetAddress\"") // Index 1 since person has 1 existing address
        assertThat(html).contains("name=\"addresses[1].city\"")
        assertThat(html).contains("name=\"addresses[1].postalCode\"")
        assertThat(html).contains("name=\"addresses[1].country\"")
        assertThat(html).contains("Add Address")
    }

    @Test
    fun `AddAddressPage lists existing addresses`() {
        // Arrange
        val page = AddAddressPage()
        val person = Person.valid(numberOfAddresses = 1) // Has 1 address to display
        val newAddress = Address(type = null, streetAddress = "", city = "", postalCode = "", country = "")

        // Act
        val html =
            createHTML().div {
                page.renderAddAddressFormContent(this, person, newAddress, emptyMap())
            }

        // Assert
        assertThat(html).contains("Existing Addresses")
        assertThat(html).contains("HOME")
        assertThat(html).contains("123 Main St")
        assertThat(html).contains("Springfield")
    }

    @Test
    fun `AddAddressPage renders existing addresses with edit links`() {
        // Arrange
        val page = AddAddressPage()
        val existingAddressId = UUID.randomUUID()
        val existingAddress =
            Address(
                id = existingAddressId,
                type = AddressType.WORK,
                streetAddress = "456 Office Blvd",
                city = "Metropolis",
                postalCode = "54321",
                country = "Canada",
            )
        val person =
            Person.valid().copy(
                addresses = listOf(existingAddress),
            )
        val newAddress = Address(type = null, streetAddress = "", city = "", postalCode = "", country = "")

        // Act
        val html =
            createHTML().div {
                page.renderAddAddressFormContent(this, person, newAddress, emptyMap())
            }

        // Assert - Should render existing address with edit link
        assertThat(html).contains("Existing Addresses")
        assertThat(html).contains("WORK: 456 Office Blvd, Metropolis")
        assertThat(html).contains("href=\"/person/${person.id}/address/$existingAddressId/edit\"")
        assertThat(html).contains("(Edit)")
    }
}
