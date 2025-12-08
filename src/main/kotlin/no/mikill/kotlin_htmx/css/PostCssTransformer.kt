package no.mikill.kotlin_htmx.css

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.IOAccess
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import kotlin.concurrent.thread

/**
 * Server-side CSS processing using PostCSS via GraalJS.
 *
 * This class enables processing CSS with PostCSS plugins (variables, nesting, autoprefixer, etc.)
 * directly on the JVM without spawning external Node.js processes. It uses GraalJS to execute
 * a Webpack-bundled PostCSS pipeline.
 *
 * ## Architecture
 *
 * ```
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │                        PostCssTransformer                           │
 * │                                                                     │
 * │  ┌─────────────────────────────────────────────────────────────┐   │
 * │  │                     Shared GraalJS Engine                    │   │
 * │  │  (JIT-compiled code cache, parsed AST structures)           │   │
 * │  └─────────────────────────────────────────────────────────────┘   │
 * │                              │                                      │
 * │       ┌──────────────────────┼──────────────────────┐              │
 * │       ▼                      ▼                      ▼              │
 * │  ┌─────────┐           ┌─────────┐           ┌─────────┐          │
 * │  │Context 1│           │Context 2│           │Context N│          │
 * │  │processCss│          │processCss│          │processCss│          │
 * │  └─────────┘           └─────────┘           └─────────┘          │
 * │       │                      │                      │              │
 * │       └──────────────────────┴──────────────────────┘              │
 * │                              │                                      │
 * │                    LinkedBlockingQueue                              │
 * │                    (thread-safe pool)                               │
 * └─────────────────────────────────────────────────────────────────────┘
 * ```
 *
 * ## Key Design Decisions
 *
 * 1. **Shared Engine**: All contexts share a single GraalJS Engine instance. This provides
 *    significant memory savings as JIT-compiled code and parsed AST structures are shared.
 *
 * 2. **Async Initialization**: Context creation is slow (~4-8 seconds per context due to
 *    JavaScript parsing and JIT compilation). Contexts are created asynchronously in a
 *    background thread so the application can start immediately.
 *
 * 3. **Pre-parsed Source**: The bundle is parsed once into a Source object and reused
 *    across all contexts, avoiding redundant parsing.
 *
 * 4. **Thread-safe Pool**: GraalJS contexts are single-threaded. We use a pool with
 *    synchronized access to safely handle concurrent requests.
 *
 * ## Performance Considerations
 *
 * - First request may block while waiting for context initialization
 * - Set appropriate HTTP caching headers for processed CSS files
 * - Consider adding an in-memory cache for frequently requested CSS
 *
 * ## Usage
 *
 * ```kotlin
 * val transformer = PostCssTransformer()
 *
 * // Process CSS with PostCSS plugins
 * val processedCss = transformer.process($$"""
 *     $primary-color: #007bff;
 *     .card {
 *         background: $primary-color;
 *         .title { font-weight: bold; }
 *     }
 * """)
 * // Result: .card { background: #007bff; } .card .title { font-weight: bold; }
 * ```
 *
 * ## Build Requirements
 *
 * The PostCSS bundle must be built before using this class:
 * ```bash
 * cd src/main/resources/postcss && npm install && npm run build
 * ```
 *
 * @param poolSize Number of GraalJS contexts to create (default: 4)
 * @see <a href="https://www.graalvm.org/latest/reference-manual/js/Multithreading/">GraalJS Multithreading</a>
 */
class PostCssTransformer(
    private val poolSize: Int = 4,
) : CssTransformer {
    private val log = LoggerFactory.getLogger(PostCssTransformer::class.java)

    /**
     * Shared engine allows contexts to share compiled code.
     * This provides significant memory savings as JIT-compiled code,
     * parsed AST structures, and internal compiler data are shared.
     */
    private val sharedEngine: Engine =
        Engine
            .newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .logHandler(HandlerSLF4JBridge(log))
            .build()

    /**
     * Pre-parse the bundle source once.
     * This Source object is shared across all contexts, avoiding redundant parsing.
     */
    private val bundleSource: Source = loadBundleSource()

    /**
     * Thread-safe pool of available contexts.
     * Contexts are added as they're created in the background thread.
     */
    private val availableContexts = LinkedBlockingQueue<Value>()

    /**
     * All created contexts for round-robin distribution.
     * Access must be synchronized via [contextLock].
     */
    private val allContexts = mutableListOf<Value>()
    private var nextContextIndex = 0
    private val contextLock = Any()

    init {
        // Start async context creation - contexts are added to pool as they become available.
        // This allows the application to start immediately while contexts initialize in background.
        thread(name = "postcss-context-init", isDaemon = true) {
            log.info("Starting async PostCSS context pool initialization ($poolSize contexts)")
            for (i in 0 until poolSize) {
                try {
                    val startTime = System.currentTimeMillis()
                    val context = createPostcssProcessFunction()
                    synchronized(contextLock) {
                        allContexts.add(context)
                    }
                    availableContexts.offer(context)
                    val elapsed = System.currentTimeMillis() - startTime
                    log.info("PostCSS context ${i + 1}/$poolSize ready (${elapsed}ms)")
                } catch (e: Exception) {
                    log.error("Failed to create PostCSS context ${i + 1}", e)
                }
            }
            log.info("PostCSS context pool initialization complete")
        }
    }

    /**
     * Extension function to process CSS using a specific GraalJS context.
     * This is synchronized to ensure only one thread uses a context at a time,
     * as GraalJS contexts are single-threaded.
     */
    @Synchronized
    fun Value.process(css: String): String {
        try {
            val processFn = getNextContext()
            val resultPromise = processFn.execute(css)

            var processedCss: String? = null
            var error: String? = null

            // Handle the JavaScript Promise returned by processCss()
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

    /**
     * Process CSS input through the PostCSS plugin pipeline.
     *
     * The CSS is processed by these plugins (in order):
     * 1. postcss-simple-vars: Resolve $variable references
     * 2. postcss-nested: Flatten nested selectors
     * 3. postcss-calc: Evaluate calc() expressions
     * 4. autoprefixer: Add vendor prefixes
     *
     * @param css Raw CSS with PostCSS syntax (variables, nesting, etc.)
     * @return Processed CSS with all transformations applied
     * @throws RuntimeException If CSS syntax is invalid or processing fails
     */
    override fun process(css: String): String = getNextContext().process(css)

    /**
     * Get the next available context using round-robin distribution.
     * If no contexts are ready yet (during startup), blocks until one is available.
     */
    private fun getNextContext(): Value {
        // First, try to get from available pool (non-blocking)
        val available = availableContexts.poll()
        if (available != null) {
            // Return it to the pool for reuse
            availableContexts.offer(available)
        }

        // Check if we have any contexts ready
        synchronized(contextLock) {
            if (allContexts.isNotEmpty()) {
                // Round-robin through all available contexts
                val index = nextContextIndex % allContexts.size
                nextContextIndex++
                return allContexts[index]
            }
        }

        // No contexts ready yet - block until first one is available
        // This only happens on the very first request during startup
        log.info("Waiting for first PostCSS context to be ready...")
        val firstContext =
            availableContexts.poll(15, TimeUnit.SECONDS)
                ?: throw IllegalStateException("Timed out waiting for PostCSS context initialization")

        // Return it to the pool
        availableContexts.offer(firstContext)
        return firstContext
    }

    /**
     * Load and pre-parse the Webpack bundle.
     * The bundle is created by running `npm run build` in src/main/resources/postcss.
     */
    private fun loadBundleSource(): Source {
        val bundleScriptContent =
            PostCssTransformer::class.java.classLoader
                .getResourceAsStream("postcss/dist/bundle.js")
                ?.bufferedReader(UTF_8)
                ?.readText()
                ?: throw IllegalArgumentException(
                    "Cannot find resource file: postcss/dist/bundle.js. " +
                        "Run 'cd src/main/resources/postcss && npm install && npm run build' to create it.",
                )
        return Source
            .newBuilder("js", bundleScriptContent, "bundle.js")
            .cached(true) // Enable code caching for faster subsequent evaluations
            .build()
    }

    /**
     * Create a new GraalJS context with the PostCSS bundle loaded.
     * Returns a reference to the processCss function.
     */
    private fun createPostcssProcessFunction(): Value {
        // Create context with shared engine - shares JIT-compiled code, AST cache, etc.
        val context =
            Context
                .newBuilder("js")
                .engine(sharedEngine) // Use shared engine for memory efficiency
                .allowExperimentalOptions(true)
                .allowIO(IOAccess.ALL)
                .allowCreateThread(false)
                .option("js.print", "true")
                .build()

        // Evaluate the pre-parsed source (faster than re-parsing)
        context.eval(bundleSource)

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
