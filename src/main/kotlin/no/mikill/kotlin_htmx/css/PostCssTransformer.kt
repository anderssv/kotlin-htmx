package no.mikill.kotlin_htmx.css

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.IOAccess
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord

/**
 * This processes CSS files on demand using PostCSS.
 *
 * The GraalJS engine is single threaded, so we create a pool of contexts.
 * The contexts are expensive to create, so it is important to reuse them.
 *
 * A PostCSS bundle is created with Webpack (src/main/resources/postcss) and includes the enabled plugins.
 *
 * This might be slow, but you should set a reasonable caching policy for CSS files.
 * If that is too slow, use a in memory cache or a file cache to store the processed CSS.
 */
class PostCssTransformer {
    private val log = LoggerFactory.getLogger(PostCssTransformer::class.java)

    // Pool contexts to avoid the slow process of creating one
    private val contextPool: List<Value> = createContextPool(2)
    private val nextContextIndex = AtomicInteger(0)

    @Synchronized
    fun Value.process(css: String): String {
        try {
            val processFn = getNextContext()
            val resultPromise = processFn.execute(css)

            var processedCss: String? = null
            var error: String? = null

            resultPromise
                .invokeMember(
                    "then",
                    ProxyExecutable { args ->
                        processedCss = args[0].asString()
                        null
                    },
                ).invokeMember(
                    "catch",
                    ProxyExecutable { args ->
                        val errorArg = args[0]
                        error =
                            if (errorArg.isString) {
                                errorArg.asString()
                            } else {
                                val errorObj = Value.asValue(errorArg)
                                if (errorObj.hasMember("stack")) {
                                    errorObj.getMember("stack").asString()
                                } else if (errorObj.hasMember("message")) {
                                    errorObj.getMember("message").asString()
                                } else {
                                    errorObj.toString()
                                }
                            }
                        null
                    },
                )

            if (error != null) {
                throw RuntimeException("PostCSS processing error from JS: $error")
            }
            return processedCss
                ?: throw IllegalStateException("PostCSS processing did not return CSS and no error was reported.")
        } catch (e: Exception) {
            throw RuntimeException("Failed to process CSS with PostCSS (Kotlin/Java layer): ${e.message}", e)
        }
    }

    fun process(css: String): String = getNextContext().process(css)

    private fun getNextContext(): Value {
        val index = nextContextIndex.getAndIncrement() % contextPool.size
        return contextPool[index]
    }

    private fun createContextPool(size: Int): List<Value> = (0 until size).map { createPostcssProcessFunction() }

    private fun createPostcssProcessFunction(): Value {
        val context =
            Context
                .newBuilder("js")
                .allowExperimentalOptions(true)
                .allowIO(IOAccess.ALL)
                .allowCreateThread(false)
                .options(
                    mutableMapOf(
                        "js.print" to "true",
                        "engine.WarnInterpreterOnly" to "false",
                    ),
                ).logHandler(HandlerSLF4JBridge(log))
                .build()

        val bundleScriptContent =
            PostCssTransformer::class.java.classLoader
                .getResourceAsStream("postcss/dist/bundle.js")
                ?.bufferedReader(UTF_8)
                ?.readText()
                ?: throw IllegalArgumentException(
                    "Cannot find resource file: ${"postcss/dist/bundle.js"}. Make sure 'npm run build' was executed.",
                )
        context.eval("js", bundleScriptContent)

        val postCssProcessorGlobal = context.getBindings("js").getMember("PostCssProcessorGlobal")
        return postCssProcessorGlobal?.getMember("processCss")
            ?: throw IllegalStateException("PostCssProcessorGlobal.processCss not found in Webpack bundle.")
    }
}

private class HandlerSLF4JBridge(
    val slf4jLogger: Logger,
) : Handler() {
    override fun publish(record: LogRecord) {
        if (!isLoggable(record)) {
            return
        }

        val message = if (formatter != null) formatter.format(record) else record.message

        val thrown = record.thrown

        if (record.level.intValue() >= Level.SEVERE.intValue()) {
            if (thrown != null) {
                slf4jLogger.error(message, thrown)
            } else {
                slf4jLogger.error(message)
            }
        } else if (record.level.intValue() >= Level.WARNING.intValue()) {
            if (thrown != null) {
                slf4jLogger.warn(message, thrown)
            } else {
                slf4jLogger.warn(message)
            }
        } else if (record.level.intValue() >= Level.INFO.intValue()) {
            if (thrown != null) {
                slf4jLogger.info(message, thrown)
            } else {
                slf4jLogger.info(message)
            }
        } else if (record.level.intValue() >= Level.FINE.intValue()) {
            if (thrown != null) {
                slf4jLogger.debug(message, thrown)
            } else {
                slf4jLogger.debug(message)
            }
        } else {
            if (thrown != null) {
                slf4jLogger.trace(message, thrown)
            } else {
                slf4jLogger.trace(message)
            }
        }
    }

    override fun flush() {
    }

    @Throws(SecurityException::class)
    override fun close() {
    }
}
