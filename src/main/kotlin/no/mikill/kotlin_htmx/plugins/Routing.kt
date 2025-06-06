package no.mikill.kotlin_htmx.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
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
            val fileName = call.parameters["fileName"]
            val resourcePath = "css/$fileName"
            val cssContent: String? =
                call::class.java.classLoader.getResourceAsStream(resourcePath)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.readText()

            if (fileName == null || cssContent == null) {
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
