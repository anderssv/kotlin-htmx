package no.mikill.kotlin_htmx.selection

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.selection.pages.SelectMainPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Endpoint tests for the selection wizard search handler (`SelectMainPage.search`).
 *
 * Wires the handler onto a minimal route because the production `configureSelectionRoutes`
 * is private and uses the HTMX routing DSL; this exercises the real branch behaviour, form
 * parameter parsing, and the HX-Redirect on success.
 *
 * The `LookupResult.Failure` branch is intentionally not covered: `LookupClient.lookup` never
 * returns it (only Success / NotFound / InvalidInput are reachable), so it is dead through the
 * real client and testing it would require production changes just to make it reachable.
 */
class SelectMainPageRoutesTest {
    private fun searchApp(block: suspend (io.ktor.client.HttpClient) -> Unit) =
        testApplication {
            val page = SelectMainPage(LookupClient("test-api-key"))
            application {
                routing {
                    post("/select/search") { searchOn(page) }
                }
            }
            block(client)
        }

    private suspend fun RoutingContext.searchOn(page: SelectMainPage) = page.search(this)

    @Test
    fun `search with a known item name sets HX-Redirect to the item page`() =
        searchApp { client ->
            val response =
                client.post("/select/search") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("lookupValue=One")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.headers["HX-Redirect"]).isEqualTo("/select/One")
        }

    @Test
    fun `search with an unknown value renders a not-found message and no redirect`() =
        searchApp { client ->
            val response =
                client.post("/select/search") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("lookupValue=DoesNotExist")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.headers["HX-Redirect"]).isNull()
            assertThat(response.bodyAsText()).contains("Could not locate item with 'DoesNotExist'.")
            assertThat(response.bodyAsText()).contains("Try again")
        }

    @Test
    fun `search with the special Invalid value renders the invalid-input message`() =
        searchApp { client ->
            val response =
                client.post("/select/search") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("lookupValue=Invalid")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.headers["HX-Redirect"]).isNull()
            assertThat(response.bodyAsText()).contains("Invalid value")
            assertThat(response.bodyAsText()).contains("Try again")
        }

    @Test
    fun `search with a missing lookupValue parameter is treated as not found`() =
        searchApp { client ->
            val response =
                client.post("/select/search") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("")
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.bodyAsText()).contains("Could not locate item with ''.")
        }
}
