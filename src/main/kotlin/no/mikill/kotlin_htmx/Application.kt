package no.mikill.kotlin_htmx

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import no.mikill.kotlin_htmx.application.ApplicationRepository
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.plugins.configureHTTP
import no.mikill.kotlin_htmx.plugins.configureMonitoring
import no.mikill.kotlin_htmx.plugins.configureRouting
import no.mikill.kotlin_htmx.plugins.configureSerialization
import java.io.File

data class ApplicationConfig(
    val lookupApiKey: String
) {

    companion object {
        fun load(): ApplicationConfig {
            // Private
            fun Map<String, String>.envOrLookup(key: String): String {
                return System.getenv(key) ?: this[key] ?: throw IllegalStateException("Missing '$key' in either env ofr env file")
            }

            val envVars: Map<String, String> = envFile().let { envFile ->
                if (envFile?.exists() == true) {
                    envFile.readLines()
                        .map { it.split("=") }
                        .filter { it.size == 2 }
                        .associate { it.first().trim() to it.last().trim() }
                } else emptyMap()
            }

            return ApplicationConfig(
                lookupApiKey = envVars.envOrLookup("LOOKUP_API_KEY")
            )
        }

    }
}

fun envFile(): File? {
    // I don't really recommend having this default env file, but do it now to ease testing of example app
    // Settings in ENV will override file always
    return listOf(".env.local", ".env.default").map { File(it) }.firstOrNull { it.exists() }
}

fun main() {
    // Have to do this before the rest of the loading of KTor. I guess it's because it does something fancy
    // with the classloader to be able to do hot reload.
    if (envFile()?.readText()?.contains("KTOR_DEVELOPMENT=true") == true) System.setProperty(
        "io.ktor.development",
        "true"
    )
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRouting()
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 0.9
        }
    }

    // Manual dependency injection :) Usually smart to find a separate place to do this from KTor
    val config = ApplicationConfig.load()

    // Load pages
    configurePageRoutes(
        LookupClient(config.lookupApiKey),
        ApplicationRepository()
    )
}
