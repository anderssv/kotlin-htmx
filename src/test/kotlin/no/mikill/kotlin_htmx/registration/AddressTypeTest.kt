package no.mikill.kotlin_htmx.registration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AddressTypeTest {
    @Test
    fun `AddressType enum should have HOME WORK OTHER values`() {
        // Arrange & Act
        val values = AddressType.entries.toTypedArray()

        // Assert
        assertThat(values).containsExactlyInAnyOrder(
            AddressType.HOME,
            AddressType.WORK,
            AddressType.OTHER,
        )
    }
}
