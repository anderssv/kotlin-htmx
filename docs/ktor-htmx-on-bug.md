# Ktor HTMX DSL Bug: Event Handler `on()` Function

## Summary

The Ktor 3.4.0 HTMX DSL `on()` function generates **incorrect HTML attributes** for HTMX event handlers, causing them to not work in the browser.

## Environment

- **Ktor Version:** 3.4.0
- **HTMX Version:** 2.0.3
- **Affected Module:** `io.ktor:ktor-htmx-html:3.4.0`

## The Bug

### What Ktor Generates (BROKEN)

**Kotlin DSL:**
```kotlin
attributes.hx {
    on("after-request", "if(event.detail.successful) this.reset()")
}
```

**Generated HTML:**
```html
<form hx-on:after-request="if(event.detail.successful) this.reset()">
```

**Problem:** Uses single colon without `htmx:` prefix → Browser doesn't recognize this as an HTMX event

### What HTMX 2.0 Requires (CORRECT)

**Option 1 - Double colon shorthand (recommended):**
```html
<form hx-on::after-request="if(event.detail.successful) this.reset()">
```

**Option 2 - Single colon with full event name:**
```html
<form hx-on:htmx:after-request="if(event.detail.successful) this.reset()">
```

### Workaround (WORKS)

```kotlin
// Manual attribute setting with double colon
attributes["hx-on::after-request"] = "if(event.detail.successful) this.reset()"
```

**Generated HTML:**
```html
<form hx-on::after-request="if(event.detail.successful) this.reset()">
```

## Root Cause

The Ktor source code implementation in `HxAttributes.kt` (lines 63-65):

```kotlin
public fun on(event: String, script: String) {
    map["hx-on:$event"] = script  // ❌ Missing htmx: prefix OR second colon
}
```

**Should be:**
```kotlin
public fun on(event: String, script: String) {
    map["hx-on::$event"] = script  // ✅ Use double colon for HTMX events
}
```

## HTMX 2.0 Event Handler Syntax

According to [HTMX documentation](https://htmx.org/attributes/hx-on/), HTMX 2.0 supports two formats:

### 1. Single Colon (for all events)
```html
<!-- DOM event -->
<button hx-on:click="alert('Clicked!')">Click</button>

<!-- HTMX event with full name -->
<button hx-on:htmx:before-request="alert('Request!')">Submit</button>
```

### 2. Double Colon (shorthand for HTMX events)
```html
<!-- HTMX event - automatically prefixed with htmx: -->
<button hx-on::before-request="alert('Request!')">Submit</button>
```

**Key Point:** `hx-on::before-request` is shorthand for `hx-on:htmx:before-request`

## Evidence

### Test Verification

Running the HTMX Questions Page test:

**With Ktor DSL `on()` function:**
```
HtmxQuestionsPageTest > testHtmxQuestionsPage() FAILED
  Expecting empty but was: "This is a test question 1771157886348"
```
❌ Form not reset → event handler not working

**With manual attribute:**
```
HtmxQuestionsPageTest > testHtmxQuestionsPage() PASSED
```
✅ Form resets correctly → event handler working

### HTML Comparison

**Manual attribute (working):**
```bash
$ curl -s http://localhost:8080/demo/htmx/questions | grep hx-on
<form id="question-form" hx-on::after-request="if(event.detail.successful) this.reset()">
```

**Ktor DSL (broken):**
```bash
$ curl -s http://localhost:8080/demo/htmx/questions | grep hx-on
<form id="question-form" hx-on:after-request="if(event.detail.successful) this.reset()">
```

## Impact

Any HTMX event handlers set via the Ktor DSL `on()` function will silently fail. This affects:
- HTMX lifecycle events: `before-request`, `after-request`, `before-swap`, etc.
- Custom HTMX events
- Any event that should be prefixed with `htmx:`

DOM events (like `click`, `submit`) may work with single colon, but HTMX-specific events won't.

## Recommended Fix

### Option 1: Use Double Colon (Simpler)
```kotlin
public fun on(event: String, script: String) {
    map["hx-on::$event"] = script
}
```

### Option 2: Detect HTMX Events (More Complete)
```kotlin
public fun on(event: String, script: String) {
    val htmxEvents = setOf(
        "abort", "after-on-load", "after-process-node", "after-request",
        "after-settle", "after-swap", "before-cleanup-element", "before-on-load",
        "before-process-node", "before-request", "before-send", "before-swap",
        "before-transition", "config-request", "confirm", "history-cache-miss",
        "history-cache-miss-error", "history-cache-miss-load", "history-restore",
        "load", "no-sse-source-error", "on-load-error", "oob-after-swap",
        "oob-before-swap", "oob-error-no-target", "prompt", "push-url",
        "re-swap-response", "replace-url", "response-error", "send-error",
        "sse-error", "sse-open", "swap-error", "target-error", "timeout",
        "trigger", "validation-validate", "validation-failed", "validation-halted",
        "xhr-abort", "xhr-load-end", "xhr-load-start", "xhr-progress"
    )

    val attribute = if (event in htmxEvents) {
        "hx-on::$event"  // Use shorthand for HTMX events
    } else {
        "hx-on:$event"   // Use single colon for DOM events
    }
    map[attribute] = script
}
```

### Option 3: Bracket Notation Fix
The `On` value class already has the same bug:
```kotlin
@JvmInline
public value class On(private val attributes: MutableMap<String, String>) {
    public operator fun set(event: String, script: String?) {
        if (script == null) {
            attributes.remove("${HxAttributeKeys.On}:$event")  // ❌
        } else {
            attributes["${HxAttributeKeys.On}:$event"] = script  // ❌
        }
    }
}
```

Should be:
```kotlin
public operator fun set(event: String, script: String?) {
    if (script == null) {
        attributes.remove("${HxAttributeKeys.On}::$event")  // ✅
    } else {
        attributes["${HxAttributeKeys.On}::$event"] = script  // ✅
    }
}
```

## Current Workaround in Project

All event handlers use manual attributes:

```kotlin
// src/main/kotlin/no/mikill/kotlin_htmx/pages/htmx/HtmxQuestionsPage.kt:115
attributes["hx-on::after-request"] = "if(event.detail.successful) this.reset()"
```

## Next Steps

1. ✅ Documented the bug
2. ⬜ File issue on [Ktor GitHub](https://github.com/ktorio/ktor/issues)
3. ⬜ Consider submitting a PR with the fix
4. ⬜ Update code to use DSL once fixed (in future Ktor version)

## References

- [HTMX hx-on Attribute Documentation](https://htmx.org/attributes/hx-on/)
- [HTMX 2.0 Migration Guide](https://htmx.org/migration-guide-htmx-1/)
- [Ktor HTMX Integration Docs](https://ktor.io/docs/htmx-integration.html)
- [Ktor HxAttributes Source](https://github.com/ktorio/ktor/blob/3.4.0/ktor-shared/ktor-htmx/ktor-htmx-html/common/src/io/ktor/htmx/html/HxAttributes.kt)

---

**Analysis Date:** 2026-02-15
**Analyzed By:** Claude Sonnet 4.5 & User
**Ktor Version:** 3.4.0 (latest as of analysis date)
