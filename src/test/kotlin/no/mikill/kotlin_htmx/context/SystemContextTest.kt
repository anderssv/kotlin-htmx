package no.mikill.kotlin_htmx.context

import no.mikill.kotlin_htmx.ApplicationConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SystemContextTest {
    @Test
    fun `SystemContext provides all dependencies`() {
        val config = ApplicationConfig(lookupApiKey = "test-key")
        val context = SystemContext(config, numberOfCheckboxes = 5)

        assertThat(context.repositories.personRepository).isNotNull
        assertThat(context.services.validationService).isNotNull
        assertThat(context.clients.lookupClient).isNotNull
        assertThat(context.numberOfCheckboxes).isEqualTo(5)
    }
}
