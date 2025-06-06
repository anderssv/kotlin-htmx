package no.mikill.kotlin_htmx

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import no.mikill.kotlin_htmx.application.ApplicationRepository
import no.mikill.kotlin_htmx.css.PostCssTransformer
import no.mikill.kotlin_htmx.integration.LookupClient
import no.mikill.kotlin_htmx.plugins.configureHTTP
import no.mikill.kotlin_htmx.plugins.configureMonitoring
import no.mikill.kotlin_htmx.plugins.configureRouting
import no.mikill.kotlin_htmx.plugins.configureSerialization
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Configuration class for the application.
 * Loads configuration from environment variables or .env files.
 *
 * @property lookupApiKey API key for the lookup service
 */
data class ApplicationConfig(
    val lookupApiKey: String
) {
    companion object {
        /**
         * Loads the application configuration from environment variables or .env files.
         * Environment variables take precedence over .env file values.
         *
         * @return Configured ApplicationConfig instance
         * @throws IllegalStateException if required configuration values are missing
         */
        fun load(): ApplicationConfig {
            /**
             * Gets a value from environment variables or the provided map.
             * Environment variables take precedence.
             *
             * @param key The configuration key to look up
             * @return The value for the key
             * @throws IllegalStateException if the key is not found in either source
             */
            fun Map<String, String>.envOrLookup(key: String): String {
                return System.getenv(key) ?: this[key]
                ?: throw IllegalStateException("Missing '$key' in either env or env file")
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

/**
 * Finds and returns the first existing environment file from the predefined list.
 * Checks for .env.local first, then falls back to .env.default.
 *
 * Note: Using a default env file is convenient for testing but not recommended for production.
 * Environment variables should always take precedence over file-based configuration.
 *
 * @return The first existing environment file, or null if none exists
 */
fun envFile(): File? {
    return listOf(".env.local", ".env.default").map { File(it) }.firstOrNull { it.exists() }
}

val logger = LoggerFactory.getLogger("no.mikill.kotlin_htmx.ApplicationKt")!!

/**
 * Application entry point that starts the Ktor server.
 * Sets up development mode if specified in the environment file.
 */
fun main() {
    // Print the memory configuration for debugging purposes
    logger.info("Max memory: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB")
    logger.info("Total memory: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
    logger.info("Free memory: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB")
    logger.info("Available processors: ${Runtime.getRuntime().availableProcessors()}")
    logger.info("Environment file: ${envFile()?.absolutePath}")

    // Set development mode property before Ktor initialization
    // This needs to be done early because Ktor configures the classloader for hot reloading
    if (envFile()?.readText()?.contains("KTOR_DEVELOPMENT=true") == true) {
        System.setProperty("io.ktor.development", "true")
    }

    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

/**
 * Configures the Ktor application module with all necessary components.
 * Sets up HTTP, monitoring, serialization, routing, and compression.
 * Initializes dependencies and configures page routes.
 */
fun Application.module() {
    val numberOfCheckboxes = System.getenv("NUMBER_OF_BOXES")?.toInt() ?: 5000
    // Configure Ktor features
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRouting(PostCssTransformer())

    // Enable compression for better performance
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

    // Configure page routes with dependencies
    configurePageRoutes(
        LookupClient(config.lookupApiKey),
        ApplicationRepository(),
        numberOfCheckboxes
    )
}
