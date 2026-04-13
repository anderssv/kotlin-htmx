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
            .finally(function() { setTimeout(poll, 1000); });
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

val LiveReload =
    createApplicationPlugin(name = "LiveReload") {
        val logger = LoggerFactory.getLogger("LiveReload")
        val version = System.currentTimeMillis().toString()

        application.routing {
            get("/__dev/reload") {
                // Close connection after each response to prevent keep-alive pooling.
                // Without this, after Ktor auto-reload the browser's pooled connection
                // stays bound to the old cancelled module, causing permanent 500s.
                call.response.header(HttpHeaders.Connection, "close")
                call.respondText("""{"version":"$version"}""", ContentType.Application.Json)
            }
        }

        logger.info("LiveReload plugin installed — polling for changes every 1s")

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
