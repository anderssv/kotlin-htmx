package no.mikill.kotlin_htmx

import io.ktor.server.sse.ServerSSESession
import kotlinx.coroutines.runBlocking
import no.mikill.kotlin_htmx.pages.htmx.HtmxCheckboxDemoPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HtmxCheckboxDemoPageTest {
    @Test
    fun `should return connected client count`() {
        val page = HtmxCheckboxDemoPage(numberOfBoxes = 100)

        val count = page.getConnectedClientCount()

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `should broadcast client count when new client registers`() {
        runBlocking {
            val page = HtmxCheckboxDemoPage(numberOfBoxes = 100)
            val capturedEvents = mutableListOf<Pair<String, String>>() // Pair of (data, event)

            val existingSession = FakeServerSSESession(capturedEvents)
            page.registerOnCheckBoxNotification(existingSession)

            // Clear initial registration broadcasts
            capturedEvents.clear()

            // Register a new session
            val newSession = FakeServerSSESession(capturedEvents)
            page.registerOnCheckBoxNotification(newSession)

            // Verify that a client count update was broadcast
            assertThat(capturedEvents).isNotEmpty
            val clientCountEvent = capturedEvents.find { it.second == "update-client-count" }
            assertThat(clientCountEvent).isNotNull
            assertThat(clientCountEvent!!.first).contains("Connected browsers: 2")
        }
    }
}

class FakeServerSSESession(
    private val capturedEvents: MutableList<Pair<String, String>>,
) : ServerSSESession {
    override suspend fun send(
        data: String?,
        event: String?,
        id: String?,
        retry: Long?,
        comments: String?,
    ) {
        capturedEvents.add(Pair(data ?: "", event ?: ""))
    }

    override suspend fun send(event: io.ktor.sse.ServerSentEvent) {
        capturedEvents.add(Pair(event.data ?: "", event.event ?: ""))
    }

    override suspend fun close() {
        // No-op for fake
    }

    override val call: io.ktor.server.application.ApplicationCall
        get() = throw UnsupportedOperationException("Not implemented for fake")

    override val coroutineContext: kotlin.coroutines.CoroutineContext
        get() = throw UnsupportedOperationException("Not implemented for fake")
}
