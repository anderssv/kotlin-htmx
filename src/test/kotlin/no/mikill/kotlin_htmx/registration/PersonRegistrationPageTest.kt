package no.mikill.kotlin_htmx.registration

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonRegistrationPageTest {
    @Test
    fun `PersonRegistrationPage renders person form with PropertyPath inputs`() {
        // Arrange
        val page = PersonRegistrationPage()
        val person = Person(firstName = "", lastName = "", email = "", addresses = emptyList())

        // Act
        val html =
            createHTML().div {
                page.renderPersonFormContent(this, person, emptyMap())
            }

        // Assert
        assertThat(html).contains("Register Person")
        assertThat(html).contains("name=\"firstName\"")
        assertThat(html).contains("name=\"lastName\"")
        assertThat(html).contains("name=\"email\"")
        assertThat(html).contains("Continue to Addresses")
    }
}
