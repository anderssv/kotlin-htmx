package no.mikill.kotlin_htmx.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.calllogging.CallLoggingConfig
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.atomic.AtomicLong

private const val IDIOMORPH_SCRIPT =
    """<script src="https://unpkg.com/idiomorph@0.7.4/dist/idiomorph.min.js"></script>"""

private const val LIVE_RELOAD_SCRIPT = """<script>
(function() {
    var lastVersion = null;
    function poll() {
        fetch('/__dev/reload')
            .then(function(r) { return r.ok ? r.json() : Promise.reject('not ok'); })
            .then(function(data) {
                if (lastVersion !== null && data.version !== lastVersion) {
                    morphPage();
                }
                lastVersion = data.version;
            })
            .catch(function() {})
            .finally(function() { setTimeout(poll, 500); });
    }
    function morphPage() {
        fetch(location.href)
            .then(function(r) { return r.ok ? r.text() : Promise.reject('fetch failed'); })
            .then(function(html) {
                var doc = new DOMParser().parseFromString(html, 'text/html');
                Idiomorph.morph(document.head, doc.head, {morphStyle: 'innerHTML'});
                Idiomorph.morph(document.body, doc.body, {morphStyle: 'innerHTML'});
            })
            .catch(function(e) {
                console.warn('[LiveReload] Morph failed, falling back to full reload:', e);
                location.reload();
            });
    }
    poll();
})();
</script>"""

/**
 * Watches a directory for file changes using Java NIO WatchService.
 * Bumps a version counter whenever a file matching the given extensions is modified.
 */
private class FileWatcher(
    private val directory: Path,
    private val extensions: Set<String>,
) {
    private val log = LoggerFactory.getLogger("FileWatcher")
    private val version = AtomicLong(0)

    init {
        val watchService = FileSystems.getDefault().newWatchService()
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE,
        )

        val thread =
            Thread({
                while (true) {
                    val key = watchService.take()
                    var changed = false
                    for (event in key.pollEvents()) {
                        val filename = (event.context() as? Path)?.fileName?.toString() ?: continue
                        val ext = filename.substringAfterLast('.', "")
                        if (ext in extensions) {
                            changed = true
                        }
                    }
                    if (changed) {
                        val newVersion = version.incrementAndGet()
                        log.info("File change detected in $directory — version $newVersion")
                    }
                    key.reset()
                }
            }, "livereload-file-watcher")
        thread.isDaemon = true
        thread.start()

        log.info("Watching $directory for changes to ${extensions.joinToString(", ") { "*.$it" }}")
    }

    fun currentVersion(): Long = version.get()
}

class LiveReloadConfig {
    /** Directories to watch for file changes. Empty list disables file watching. */
    var watchPaths: List<String> = emptyList()

    /** File extensions to watch for changes. */
    var watchExtensions: Set<String> = setOf("css")
}

val LiveReload =
    createApplicationPlugin(name = "LiveReload", createConfiguration = ::LiveReloadConfig) {
        val logger = LoggerFactory.getLogger("LiveReload")
        val kotlinVersion = System.currentTimeMillis().toString()

        val fileWatchers =
            pluginConfig.watchPaths.mapNotNull { path ->
                val watchDir = File(path)
                if (watchDir.isDirectory) {
                    FileWatcher(watchDir.toPath(), extensions = pluginConfig.watchExtensions)
                } else {
                    logger.warn("LiveReload watch path '$path' is not a directory — skipping")
                    null
                }
            }

        application.routing {
            get("/__dev/reload") {
                val fileVersion = fileWatchers.sumOf { it.currentVersion() }
                val combinedVersion = "$kotlinVersion-$fileVersion"
                // Close connection after each response to prevent keep-alive pooling.
                // Without this, after Ktor auto-reload the browser's pooled connection
                // stays bound to the old cancelled module, causing permanent 500s.
                call.response.header(HttpHeaders.Connection, "close")
                call.respondText("""{"version":"$combinedVersion"}""", ContentType.Application.Json)
            }
        }

        logger.info("LiveReload plugin installed — polling for changes every 1s (Kotlin + CSS)")

        onCallRespond { _ ->
            transformBody { data ->
                if (data is TextContent && data.contentType.withoutParameters() == ContentType.Text.Html) {
                    val modifiedBody =
                        data.text
                            .replace("</head>", "$IDIOMORPH_SCRIPT</head>")
                            .replace("</body>", "$LIVE_RELOAD_SCRIPT</body>")
                    TextContent(modifiedBody, data.contentType, data.status)
                } else {
                    data
                }
            }
        }
    }

/**
 * Excludes the LiveReload polling endpoint (/__dev/reload) from call logging.
 * This endpoint polls every 1s and generates noise in the logs.
 */
fun CallLoggingConfig.excludeDevReloadEndpoint() {
    filter { call -> !call.request.path().startsWith("/__dev/") }
}
