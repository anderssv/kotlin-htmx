package no.mikill.kotlin_htmx.integration

import io.ktor.test.dispatcher.*
import no.mikill.kotlin_htmx.ApplicationConfig
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class LookupClientTest {
    private val config = ApplicationConfig.load()
    private val client = LookupClient(config.lookupApiKey)

    @Test
    fun shouldLookupCorrect() = testSuspend {
        val result = client.lookup("One") as LookupResult.Success
        assertThat(result.response).isEqualToIgnoringCase("One")
    }

    @Test
    fun shouldHandleBadCharactersValue() = testSuspend {
        client.lookup("Invalid") as LookupResult.InvalidInput
    }
}