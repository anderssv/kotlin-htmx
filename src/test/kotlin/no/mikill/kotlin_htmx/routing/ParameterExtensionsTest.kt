package no.mikill.kotlin_htmx.routing

import io.ktor.http.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class ParameterExtensionsTest {
    @Test
    fun `getUUID should extract valid UUID from parameters`() {
        // Arrange
        val expectedUuid = UUID.randomUUID()
        val parameters =
            Parameters.build {
                append("id", expectedUuid.toString())
            }

        // Act
        val result = parameters.getUUID("id")

        // Assert
        assertThat(result).isEqualTo(expectedUuid)
    }

    @Test
    fun `getUUID should throw IllegalArgumentException when parameter is missing`() {
        // Arrange
        val parameters = Parameters.build {}

        // Act & Assert
        assertThatThrownBy { parameters.getUUID("id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Parameter id is required")
    }

    @Test
    fun `getUUID should throw IllegalArgumentException when UUID is invalid`() {
        // Arrange
        val parameters =
            Parameters.build {
                append("id", "not-a-valid-uuid")
            }

        // Act & Assert
        assertThatThrownBy { parameters.getUUID("id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Invalid UUID in parameter: id")
    }
}
