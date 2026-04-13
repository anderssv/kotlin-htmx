# Auto-Refresh in the Browser with Ktor

This document explains what it takes to get automatic browser refresh working during development with Ktor. The setup has three cooperating parts: Gradle continuous compilation, Ktor auto-reload, and a custom LiveReload plugin that updates the browser.

### Relevant Files

| File | Role |
|------|------|
| [`build.gradle.kts`](../build.gradle.kts) | Enables Ktor development mode (`ktor { development = true }`) |
| [`Application.kt`](../src/main/kotlin/no/mikill/kotlin_htmx/Application.kt) | Server setup with `watchPaths` and suspend module reference |
| [`plugins/LiveReload.kt`](../src/main/kotlin/no/mikill/kotlin_htmx/plugins/LiveReload.kt) | Custom plugin: version endpoint, script injection, DOM morphing |
| [`plugins/Monitoring.kt`](../src/main/kotlin/no/mikill/kotlin_htmx/plugins/Monitoring.kt) | Filters LiveReload polling from call logs |
| [`start-dev-server.sh`](../start-dev-server.sh) | Launches both server and continuous compiler |
| [`plugins/LiveReloadTest.kt`](../src/test/kotlin/no/mikill/kotlin_htmx/plugins/LiveReloadTest.kt) | Tests for the LiveReload plugin |

## The Three Parts

```
Developer saves a file
        |
        v
1. Gradle continuous build    (recompiles changed classes)
        |
        v
2. Ktor auto-reload           (detects new classes, reloads the module)
        |
        v
3. LiveReload plugin           (browser polls for changes, morphs DOM)
```

All three must be in place. Without Gradle watching, nothing recompiles. Without Ktor auto-reload, the server keeps running old code. Without LiveReload, the developer has to manually refresh the browser.

---

## 1. Gradle Continuous Compilation

Gradle's `-t` (continuous) flag watches source files and recompiles on change:

```bash
./gradlew -t compileKotlin -x test
```

This runs in a separate process from the server. It watches `src/` for changes and recompiles into `build/classes/`.

**Important**: Do NOT use `./gradlew run -t`. The `-t` flag on `run` restarts the entire server process on every change, which is slow and loses all state. The correct approach is two separate processes:

- `./gradlew run` — runs the server
- `./gradlew -t compileKotlin -x test` — watches and recompiles

The [`start-dev-server.sh`](../start-dev-server.sh) script handles starting both processes automatically.

## 2. Ktor Auto-Reload (Development Mode)

Ktor can watch output directories for changed class files and reload the application module without restarting the server. This requires three things:

### a. Enable development mode in [`build.gradle.kts`](../build.gradle.kts)

```kotlin
ktor {
    development = true
}
```

This tells the Ktor Gradle plugin to set `Application.developmentMode = true` at runtime. Without this, Ktor won't watch for changes.

### b. Configure `watchPaths` in the server setup ([`Application.kt`](../src/main/kotlin/no/mikill/kotlin_htmx/Application.kt))

```kotlin
embeddedServer(
    Netty,
    port = port,
    host = "0.0.0.0",
    watchPaths = listOf("classes", "resources"),
    module = Application::module,
).start(wait = true)
```

`watchPaths` tells Ktor which directories under `build/` to monitor. When files change in `build/classes/` or `build/resources/`, Ktor triggers a module reload.

### c. Use a `suspend` function reference for the module

```kotlin
suspend fun Application.module() {
    // ...
}
```

The module must be:
- A **suspend** function (not a regular function)
- Passed as a **function reference** (`Application::module`), not a lambda

This is a requirement in Ktor 3.2+. Lambda initializers (`module = { ... }`) and non-suspend function references will silently break auto-reload.

### How the reload works

When Ktor detects changed class files, it creates a new classloader and calls `Application.module()` again. All plugins are re-installed, all routes are re-registered. The server socket stays open — there is no restart, no port rebinding, no downtime.

## 3. LiveReload Plugin (Browser Update)

Ktor auto-reload updates the server, but the browser still shows the old page. The custom LiveReload plugin in [`plugins/LiveReload.kt`](../src/main/kotlin/no/mikill/kotlin_htmx/plugins/LiveReload.kt) handles the browser side.

### How it works

When the plugin is installed, it:

1. **Generates a version** from `System.currentTimeMillis()`. Each time Ktor reloads the module, a new plugin instance is created with a new timestamp.

2. **Registers a version endpoint** at `GET /__dev/reload` that returns `{"version":"<timestamp>"}`.

3. **Injects a polling script** into every HTML response. The script:
   - Polls `/__dev/reload` every 1 second
   - When the version changes, fetches the current page
   - Uses [Idiomorph](https://github.com/bigskysoftware/idiomorph) to morph the DOM in-place (preserving scroll position, form state, focus)
   - Falls back to a full `location.reload()` if morphing fails

### Conditional installation

The plugin is only installed in development mode (in [`Application.kt`](../src/main/kotlin/no/mikill/kotlin_htmx/Application.kt)):

```kotlin
if (developmentMode) {
    install(LiveReload)
}
```

This ensures no polling scripts or version endpoints exist in production.

### Connection: close header

The version endpoint sets `Connection: close` on every response:

```kotlin
call.response.header(HttpHeaders.Connection, "close")
```

Without this, after Ktor auto-reload the browser's pooled HTTP connection stays bound to the old module, causing 500 errors on subsequent polls.

### Log filtering

The polling endpoint fires every second, which floods the logs. The plugin provides a filter, used in [`plugins/Monitoring.kt`](../src/main/kotlin/no/mikill/kotlin_htmx/plugins/Monitoring.kt):

```kotlin
install(CallLogging) {
    excludeDevReloadEndpoint()
}
```

This suppresses `/__dev/reload` requests from call logging.

## Using the Dev Server

The simplest way to start everything with [`start-dev-server.sh`](../start-dev-server.sh):

```bash
./start-dev-server.sh
```

This starts both the server and the continuous compiler, waits for the server to be ready, and opens the browser. See the script header for options like `PORT`, `NO_BROWSER`, and `BACKGROUND`.

To stop:

```bash
./start-dev-server.sh stop
```

Or manually in two terminals:

```bash
# Terminal 1: run the server
./gradlew run

# Terminal 2: watch and recompile
./gradlew -t compileKotlin -x test
```

## Gotchas

- **`./gradlew run -t`**: Restarts the whole server. Use two separate processes instead.
- **Lambda module initializer**: `module = { myModule() }` breaks auto-reload in Ktor 3.2+. Use a function reference.
- **Non-suspend module function**: `fun Application.module()` (without `suspend`) breaks auto-reload in Ktor 3.2+.
- **Missing `watchPaths`**: Without this, Ktor won't detect recompiled classes.
- **Missing `development = true`**: Without this in `build.gradle.kts`, `developmentMode` is false and Ktor won't watch for changes.
- **HTTP keep-alive after reload**: Pooled connections break after module reload. The `Connection: close` header on the version endpoint is required.
