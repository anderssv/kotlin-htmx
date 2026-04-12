package no.mikill.kotlin_htmx.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.CallLoggingConfig
import io.ktor.server.request.path
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        excludeDevReloadEndpoint()
        callIdMdc("call-id")
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}

/**
 * Excludes the Ktor auto-reload polling endpoint (/__dev/reload) from call logging.
 * This endpoint is added by the Ktor Gradle plugin when development mode is enabled
 * and generates noise in the logs.
 */
fun CallLoggingConfig.excludeDevReloadEndpoint() {
    filter { call -> !call.request.path().startsWith("/__dev/") }
}
