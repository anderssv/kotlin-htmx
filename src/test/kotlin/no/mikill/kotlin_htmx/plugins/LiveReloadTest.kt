package no.mikill.kotlin_htmx.plugins

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LiveReloadTest {
    @Test
    fun `HTML responses include live-reload polling script when plugin is installed`() =
        testApplication {
            application {
                install(LiveReload)
                routing {
                    get("/test") {
                        call.respondText(
                            "<html><head></head><body><h1>Hello</h1></body></html>",
                            ContentType.Text.Html,
                        )
                    }
                }
            }

            val response = client.get("/test")

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.bodyAsText()
            assertThat(body).contains("/__dev/reload")
            assertThat(body).contains("Idiomorph.morph")
            assertThat(body).contains("</body>")
        }

    @Test
    fun `HTML responses include idiomorph script in head`() =
        testApplication {
            application {
                install(LiveReload)
                routing {
                    get("/test") {
                        call.respondText(
                            "<html><head><title>Test</title></head><body><h1>Hello</h1></body></html>",
                            ContentType.Text.Html,
                        )
                    }
                }
            }

            val response = client.get("/test")

            val body = response.bodyAsText()
            assertThat(body).contains("idiomorph")
            // idiomorph script should be in the head section (before </head>)
            val idiomorphPos = body.indexOf("idiomorph")
            val headClosePos = body.indexOf("</head>")
            assertThat(idiomorphPos).isLessThan(headClosePos)
        }

    @Test
    fun `non-HTML responses are not modified`() =
        testApplication {
            val jsonContent = """{"key": "value"}"""
            application {
                install(LiveReload)
                routing {
                    get("/api") {
                        call.respondText(jsonContent, ContentType.Application.Json)
                    }
                }
            }

            val response = client.get("/api")

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.bodyAsText()
            assertThat(body).isEqualTo(jsonContent)
            assertThat(body).doesNotContain("/__dev/reload")
        }

    @Test
    fun `polling endpoint returns JSON with version field`() =
        testApplication {
            application {
                install(LiveReload)
            }

            val response = client.get("/__dev/reload")

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.headers["Content-Type"]).contains("application/json")
            val body = response.bodyAsText()
            assertThat(body).contains("\"version\"")
        }

    @Test
    fun `polling endpoint sets Connection close to prevent keep-alive pooling issues on auto-reload`() =
        testApplication {
            application {
                install(LiveReload)
            }

            val response = client.get("/__dev/reload")

            assertThat(response.headers["Connection"]).isEqualTo("close")
        }

    @Test
    fun `polling endpoint returns same version on repeated calls within same module lifecycle`() =
        testApplication {
            application {
                install(LiveReload)
            }

            val body1 = client.get("/__dev/reload").bodyAsText()
            val body2 = client.get("/__dev/reload").bodyAsText()

            assertThat(body1).isEqualTo(body2)
        }
}
