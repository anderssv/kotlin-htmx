package no.mikill.kotlin_htmx.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import no.mikill.kotlin_htmx.css.PostCssTransformer
import org.slf4j.LoggerFactory

fun Application.configureRouting(postCssTransformer: PostCssTransformer) {
    val logger = LoggerFactory.getLogger("Routing")
    install(SSE)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled error", cause)
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/css/{fileName}") {
            fun locateAndReadCssFile(fileName: String): String? {
                // Supporting both .css and .scss files for convenience and correct interpretation in IDEs
                // The browser requests .css, but we can also process .scss files
                val resourcePaths =
                    listOf(
                        "css/$fileName",
                        "css/${fileName.replace(".css", ".scss", true)}",
                    )

                // Find the first existing resource and read its content
                val resourcePath =
                    resourcePaths.firstOrNull {
                        call::class.java.classLoader.getResource(it) != null
                    }

                return resourcePath?.let {
                    call::class.java.classLoader
                        .getResourceAsStream(resourcePath)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.readText()
                }
            }

            val fileName = call.parameters["fileName"]!!
            val cssContent: String? = locateAndReadCssFile(fileName)

            if (cssContent == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                val processedCss = postCssTransformer.process(cssContent)
                call.respondText(processedCss, ContentType.Text.CSS)
            }
        }

        staticResources("/static", "static") {
            exclude { it.path.endsWith(".css") }
        }
        staticResources("/script", "script")
    }
}
