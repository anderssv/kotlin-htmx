package no.mikill.kotlin_htmx.css

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * CSS transformer using LightningCSS native binary.
 *
 * LightningCSS is a Rust-based CSS parser, transformer, and minifier that is ~100x faster
 * than PostCSS/GraalVM. It handles CSS nesting, autoprefixing, and minification.
 *
 * The binary is installed via mise (cargo:lightningcss with cli feature).
 * In Docker, the binary is downloaded from npm registry.
 */
class LightningCssTransformer(
    private val binaryPath: String = findBinaryPath(),
    private val targets: String = ">= 0.25%",
    private val minify: Boolean = false,
) : CssTransformer {
    private val log = LoggerFactory.getLogger(LightningCssTransformer::class.java)

    init {
        log.info("LightningCSS transformer initialized with binary: $binaryPath")
    }

    override fun process(cssContent: String): String {
        val command =
            buildList {
                add(binaryPath)
                add("--targets")
                add(targets)
                if (minify) {
                    add("--minify")
                }
            }

        val process =
            ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

        // Write CSS to stdin
        process.outputStream.bufferedWriter().use { writer ->
            writer.write(cssContent)
        }

        // Read result from stdout
        val result = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        val exitCode = process.waitFor(30, TimeUnit.SECONDS)
        if (!exitCode || process.exitValue() != 0) {
            throw RuntimeException("LightningCSS processing failed: $error")
        }

        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(LightningCssTransformer::class.java)

        private fun findBinaryPath(): String {
            // Try common locations in order
            val candidates =
                listOf(
                    // mise-managed installation
                    System.getenv("HOME")?.let { "$it/.local/share/mise/shims/lightningcss" },
                    // Direct binary in PATH
                    "lightningcss",
                    // Docker/production location
                    "/usr/local/bin/lightningcss",
                )

            for (candidate in candidates.filterNotNull()) {
                try {
                    log.debug("Checking LightningCSS binary at: $candidate")
                    val process =
                        ProcessBuilder(candidate, "--version")
                            .redirectErrorStream(true)
                            .start()
                    val output = process.inputStream.bufferedReader().readText()
                    if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                        log.debug("Found working LightningCSS binary at: $candidate (version: ${output.trim()})")
                        return candidate
                    }
                    log.debug("Binary at $candidate returned exit code ${process.exitValue()}")
                } catch (e: Exception) {
                    log.debug("Binary at $candidate failed: ${e.message}")
                    // Try next candidate
                }
            }

            throw RuntimeException(
                "LightningCSS binary not found. Install via 'mise install' or ensure 'lightningcss' is in PATH. " +
                    "Tried: ${candidates.filterNotNull().joinToString(", ")}",
            )
        }
    }
}
