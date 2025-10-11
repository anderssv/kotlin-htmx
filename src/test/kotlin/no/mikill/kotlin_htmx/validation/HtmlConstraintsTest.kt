package no.mikill.kotlin_htmx.validation

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.html.InputType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HtmlConstraintsTest {
    // Test 1: @NotBlank (already implemented)
    data class PersonWithNotBlank(
        @field:NotBlank
        val name: String,
    )

    @Test
    fun `should extract required attribute from NotBlank annotation`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithNotBlank::name)

        assertThat(attributes).containsEntry("required", "")
    }

    // Test 2: @NotEmpty
    data class PersonWithNotEmpty(
        @field:NotEmpty
        val name: String,
    )

    @Test
    fun `should extract required attribute from NotEmpty annotation`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithNotEmpty::name)

        assertThat(attributes).containsEntry("required", "")
    }

    // Test 3: @NotNull
    data class PersonWithNotNull(
        @field:NotNull
        val name: String?,
    )

    @Test
    fun `should extract required attribute from NotNull annotation`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithNotNull::name)

        assertThat(attributes).containsEntry("required", "")
    }

    // Test 4: @Size with max (already implemented)
    data class PersonWithMaxSize(
        @field:Size(max = 100)
        val name: String,
    )

    @Test
    fun `should extract maxlength attribute from Size annotation`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithMaxSize::name)

        assertThat(attributes).containsEntry("maxlength", "100")
    }

    // Test 5: @Size with min (already implemented)
    data class PersonWithMinSize(
        @field:Size(min = 3)
        val name: String,
    )

    @Test
    fun `should extract minlength attribute from Size annotation`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithMinSize::name)

        assertThat(attributes).containsEntry("minlength", "3")
    }

    // Test 6: @Size with both min and max (already implemented)
    data class PersonWithMinMaxSize(
        @field:Size(min = 3, max = 100)
        val name: String,
    )

    @Test
    fun `should extract both minlength and maxlength from Size annotation`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithMinMaxSize::name)

        assertThat(attributes).containsEntry("minlength", "3")
        assertThat(attributes).containsEntry("maxlength", "100")
    }

    // Test 7: @Email (already implemented)
    data class PersonWithEmail(
        @field:Email
        val email: String,
    )

    @Test
    fun `should return email input type for Email annotation`() {
        val inputType = HtmlConstraints.getInputType(PersonWithEmail::email)

        assertThat(inputType).isEqualTo(InputType.email)
    }

    // Test 8: @Pattern
    data class PersonWithPattern(
        @field:Pattern(regexp = "[0-9]+")
        val phoneNumber: String,
    )

    @Test
    fun `should extract pattern attribute from Pattern annotation`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithPattern::phoneNumber)

        assertThat(attributes).containsEntry("pattern", "[0-9]+")
    }

    // Test 9: Multiple constraints (already implemented for existing annotations)
    data class PersonWithMultipleConstraints(
        @field:NotBlank
        @field:Size(max = 50)
        @field:Email
        val email: String,
    )

    @Test
    fun `should combine multiple constraint attributes`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithMultipleConstraints::email)

        assertThat(attributes).containsEntry("required", "")
        assertThat(attributes).containsEntry("maxlength", "50")
    }

    // Test 10: No annotations
    data class PersonWithNoAnnotations(
        val name: String,
    )

    @Test
    fun `should return empty map for property with no annotations`() {
        val attributes = HtmlConstraints.getAttributes(PersonWithNoAnnotations::name)

        assertThat(attributes).isEmpty()
    }
}
