# CURRENT FOCUS ⚡

**Status:** Core person registration system is complete. Remaining work is optional enhancements.

**Next Priority:** Optional enhancements (pick any)
- HtmlConstraints - Complete remaining annotations (@Pattern, @NotEmpty, @NotNull support)
- Type-Safe Form Components - Complete remaining component patterns
- Context Pattern - Dependency injection pattern (structural improvement)
- Routing Utilities - HtmlRenderUtils, UUID extensions (utility functions)
- Component Organization - Header/Footer extraction (structural improvement)

See "Implementation Priority" section at the bottom for full roadmap.

---
---

# TDD Plan: HtmlConstraints - Automatic HTML Attribute Generation

## Feature Description
Automatically derive HTML input attributes (required, maxlength, minlength, type, pattern) from Jakarta Bean Validation annotations on domain objects. This creates a single source of truth for validation rules.

## Implementation Approach
- Use Kotlin reflection to inspect validation annotations on properties
- Map validation annotations to HTML attributes:
  - `@NotBlank` / `@NotEmpty` / `@NotNull` → `required`
  - `@Size(max=N)` → `maxlength="N"`
  - `@Size(min=N)` → `minlength="N"`
  - `@Email` → `type="email"`
  - `@Pattern(regexp=...)` → `pattern="..."`
- Provide type-safe property references using `KProperty1<T, R>`

## Benefits
- DRY principle: Single source of truth for validation rules
- Frontend HTML5 validation automatically matches backend rules
- Type-safe: Compile-time checking for property names
- Self-documenting: Form constraints visible in domain model

---

## Tests (TDD Order)

### [ ] Test 1: Extract required attribute from @NotBlank
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given a data class with `@field:NotBlank` annotation, `HtmlConstraints.getAttributes()` should return `mapOf("required" to "")`.

**Implementation**:
```kotlin
object HtmlConstraints {
    fun <T, R> getAttributes(property: KProperty1<T, R>): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val javaField = property.javaField ?: return emptyMap()

        javaField.getAnnotation(NotBlank::class.java)?.let {
            attributes["required"] = ""
        }

        return attributes
    }
}
```

**Status**: PENDING

---

### [ ] Test 2: Extract required attribute from @NotEmpty
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given a property with `@field:NotEmpty` annotation, should return `required` attribute.

**Implementation**: Add similar check for `NotEmpty` annotation in `getAttributes()`.

**Status**: PENDING

---

### [ ] Test 3: Extract required attribute from @NotNull
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given a property with `@field:NotNull` annotation, should return `required` attribute.

**Implementation**: Add check for `NotNull` annotation.

**Status**: PENDING

---

### [✅] Test 4: Extract maxlength from @Size
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given `@field:Size(max=100)`, should return `mapOf("maxlength" to "100")`.

**Implementation**:
```kotlin
javaField.getAnnotation(Size::class.java)?.let { sizeAnnotation ->
    if (sizeAnnotation.max < Int.MAX_VALUE) {
        attributes["maxlength"] = sizeAnnotation.max.toString()
    }
}
```

**Status**: PENDING

---

### [✅] Test 5: Extract minlength from @Size
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given `@field:Size(min=3)`, should return `mapOf("minlength" to "3")`.

**Implementation**:
```kotlin
if (sizeAnnotation.min > 0) {
    attributes["minlength"] = sizeAnnotation.min.toString()
}
```

**Status**: PENDING

---

### [✅] Test 6: Extract both min and max from @Size
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given `@field:Size(min=3, max=100)`, should return both attributes.

**Implementation**: Combine both checks in Test 4 and 5.

**Status**: PENDING

---

### [✅] Test 7: Extract email type from @Email
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given `@field:Email`, should return `mapOf("type" to "email")`.

**Implementation**:
```kotlin
javaField.getAnnotation(Email::class.java)?.let {
    attributes["type"] = "email"
}
```

**Status**: PENDING

---

### [✅] Test 8: Extract pattern from @Pattern
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given `@field:Pattern(regexp="[0-9]+")`, should return `mapOf("pattern" to "[0-9]+")`.

**Implementation**:
```kotlin
javaField.getAnnotation(Pattern::class.java)?.let { patternAnnotation ->
    attributes["pattern"] = patternAnnotation.regexp
}
```

**Status**: PENDING

---

### [✅] Test 9: Combine multiple constraints
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given a field with multiple annotations (`@NotBlank @Size(max=50) @Email`), should return all corresponding HTML attributes.

**Implementation**: Already handled by combining all previous checks - verify with test.

**Status**: PENDING

---

### [✅] Test 10: Handle property with no annotations
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given a property with no validation annotations, should return empty map.

**Implementation**: Already handled by returning empty map at start - verify with test.

**Status**: PENDING

---

## Notes
- Requires `kotlin-reflect` dependency
- Must use `@field:` target for annotations to be visible on the Java field
- The pattern attribute uses raw regex - may need to adjust for HTML5 compatibility
- Not all Jakarta validation constraints map to HTML5 attributes (e.g., `@Future`, `@Past`, custom validators)
- This is a read-only utility - doesn't modify the domain model

---
---

# TDD Plan: Type-Safe Form Components with Property References

## Feature Description
Create reusable form input components that take type-safe property references (`KProperty1<T, R>`) instead of string field names. This provides compile-time safety and automatic HTML constraint generation.

## Implementation Approach
- Create `validatedTextInput()` function that accepts `KProperty1<T, R>`
- Use `HtmlConstraints.getAttributes()` to derive HTML attributes from annotations
- Create `validatedInputWithErrors()` variant that also displays inline error messages
- Support custom configuration via lambda receiver on INPUT element

## Benefits
- Type safety: Compiler catches typos in field names
- DRY: HTML constraints automatically derived from domain annotations
- Refactoring safety: Renaming fields updates all references
- Clean separation: Domain model drives both validation and UI

---

## Tests (TDD Order)

### [ ] Test 1: validatedTextInput renders basic input
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: `validatedTextInput()` should render an input element with the property name as the `name` attribute.

**Implementation**:
```kotlin
fun <T, R> DIV.validatedTextInput(
    property: KProperty1<T, R>,
    inputType: InputType,
    value: String,
    placeholder: String? = null,
    cssClasses: String? = null,
    inputId: String? = null,
    configure: INPUT.() -> Unit = {}
) {
    val name = property.name
    input(type = inputType, name = name, classes = cssClasses) {
        inputId?.let { id = it }
        this.value = value
        placeholder?.let { this.placeholder = it }
        configure()
    }
}
```

**Status**: PENDING

---

### [ ] Test 2: validatedTextInput applies HtmlConstraints
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: Given a property with `@NotBlank` annotation, the rendered input should have `required` attribute.

**Implementation**:
```kotlin
val validationAttributes = HtmlConstraints.getAttributes(property)

validationAttributes.forEach { (attrName, attrValue) ->
    when (attrName) {
        "required" -> required = true
        "maxlength" -> maxLength = attrValue
        "minlength" -> minLength = attrValue
        "pattern" -> pattern = attrValue
        "type" -> { /* handled separately */ }
    }
}
```

**Status**: PENDING

---

### [ ] Test 3: validatedTextInput respects @Email type override
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: Given a property with `@Email` annotation, should render `<input type="email">` regardless of provided inputType parameter.

**Implementation**:
```kotlin
val effectiveType = when (validationAttributes["type"]) {
    "email" -> InputType.email
    else -> inputType
}
input(type = effectiveType, name = name, classes = cssClasses) { /* ... */ }
```

**Status**: PENDING

---

### [✅] Test 4: validatedTextInput allows custom configuration
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: The `configure` lambda should allow adding custom attributes (e.g., HTMX attributes, autocomplete, etc.)

**Implementation**: Call `configure()` at end of input block - already in Test 1.

**Status**: PENDING

---

### [✅] Test 5: validatedInputWithErrors renders input
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: `validatedInputWithErrors()` should render the same input as `validatedTextInput()`.

**Implementation**:
```kotlin
fun <T, R> DIV.validatedInputWithErrors(
    property: KProperty1<T, R>,
    inputType: InputType,
    value: String,
    violations: Map<String, List<String>>,
    placeholder: String? = null,
    cssClasses: String? = null,
    inputId: String? = null,
    configure: INPUT.() -> Unit = {}
) {
    validatedTextInput(
        property = property,
        inputType = inputType,
        value = value,
        placeholder = placeholder,
        cssClasses = cssClasses,
        inputId = inputId,
        configure = configure
    )
    // Error rendering will be added in next test
}
```

**Status**: PENDING

---

### [✅] Test 6: validatedInputWithErrors renders single error
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: When violations map contains errors for the property, should render error div(s) after the input.

**Implementation**:
```kotlin
violations[property.name]?.forEach { error ->
    div(classes = "error-message") {
        +error
    }
}
```

**Status**: PENDING

---

### [✅] Test 7: validatedInputWithErrors renders multiple errors
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: When a property has multiple violations, should render multiple error divs.

**Implementation**: Already handled by forEach in Test 6 - verify with test.

**Status**: PENDING

---

### [✅] Test 8: validatedInputWithErrors renders no errors for valid field
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: When violations map doesn't contain the property, should render no error divs.

**Implementation**: Already handled by safe navigation in Test 6 - verify with test.

**Status**: PENDING

---

### [✅] Test 9: Integration test with Person data class
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: Create a test with actual `Person` data class, validate using ValidationService, and render with validatedInputWithErrors(). Verify:
- Annotations on Person properties generate correct HTML attributes
- Validation errors are displayed correctly
- Type safety works (compile error if using wrong property)

**Implementation**: Full integration test combining ValidationService + HtmlConstraints + form components.

**Status**: PENDING

---

## Notes
- Requires `kotlin-reflect` dependency
- Property references provide compile-time safety
- Error messages styled with CSS class "error-message"
- Can extend to other input types (select, textarea, checkbox, radio) following same pattern
- Consider adding form-level error summary component
- HTMX attributes can be added via `configure` lambda

---
---

# TDD Plan: Context Pattern for Dependency Injection

## Feature Description
Implement a structured dependency injection pattern using a Context object that wires together all application components (clients, services, repositories). This provides clear dependency structure and makes testing easier.

## Implementation Approach
- Create `Context` object with wire methods for each dependency layer
- Define `TopContext` data class holding all wired dependencies
- Create typed wrapper classes: `Clients`, `Services`, `Repositories`
- Support optional parameters for testing (clock, config overrides)
- Update Application.module() to use Context

## Benefits
- Clear dependency structure visible in one place
- Easy to test: can override specific dependencies
- Type-safe: All dependencies in strongly-typed containers
- Scalable: Easy to add new services/repositories
- Shows architectural best practices

---

## Tests (TDD Order)

### [ ] Test 1: Context.wireContext creates TopContext
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: `Context.wireContext()` should return a `TopContext` instance with non-null dependencies.

**Implementation**:
```kotlin
object Context {
    fun wireContext(
        clock: Clock = Clock.systemDefaultZone(),
        appConfig: ApplicationConfig = ApplicationConfig.load()
    ): TopContext {
        val clients = wireClients(appConfig)
        val repositories = wireRepositories()
        val services = wireServices(clock, clients, repositories)
        return TopContext(clock, appConfig, clients, services, repositories)
    }

    data class TopContext(
        val clock: Clock,
        val config: ApplicationConfig,
        val clients: Clients,
        val services: Services,
        val repositories: Repositories
    )

    class Clients(val lookupClient: LookupClient)
    class Repositories(val applicationRepository: ApplicationRepository)
    class Services(val validationService: ValidationService)
}
```

**Status**: PENDING

---

### [ ] Test 2: Context.wireClients creates HTTP client
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: `wireClients()` should create a `Clients` instance containing the LookupClient.

**Implementation**:
```kotlin
private fun wireClients(config: ApplicationConfig): Clients =
    Clients(
        lookupClient = LookupClient(config.lookupApiKey)
    )
```

**Status**: PENDING

---

### [ ] Test 3: Context.wireRepositories creates repositories
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: `wireRepositories()` should create a `Repositories` instance containing ApplicationRepository.

**Implementation**:
```kotlin
private fun wireRepositories(): Repositories =
    Repositories(
        applicationRepository = ApplicationRepository()
    )
```

**Status**: PENDING

---

### [✅] Test 4: Context.wireServices creates ValidationService
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: `wireServices()` should create a `Services` instance with ValidationService properly initialized.

**Implementation**:
```kotlin
private fun wireServices(
    clock: Clock,
    clients: Clients,
    repositories: Repositories
): Services {
    val validatorFactory = Validation.buildDefaultValidatorFactory()
    val validator = validatorFactory.validator
    val validationService = ValidationService(validator)
    return Services(validationService)
}
```

**Status**: PENDING

---

### [✅] Test 5: Context supports custom clock for testing
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: When passing a custom Clock to wireContext(), the TopContext should contain that clock.

**Implementation**: Already supported via default parameter - verify with test.

**Status**: PENDING

---

### [✅] Test 6: Context supports custom config for testing
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: When passing a custom ApplicationConfig, the TopContext should use it.

**Implementation**: Already supported via default parameter - verify with test.

**Status**: PENDING

---

### [✅] Test 7: Update Application.module to use Context
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ApplicationTest.kt`

**Test**: Application module should wire dependencies using Context and pass them to routes.

**Implementation**:
```kotlin
fun Application.module() {
    val context = Context.wireContext()

    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRouting(PostCssTransformer())

    install(Compression) {
        gzip { priority = 1.0 }
        deflate { priority = 0.9 }
    }

    configurePageRoutes(
        context.clients.lookupClient,
        context.repositories.applicationRepository,
        context.services.validationService,
        numberOfCheckboxes
    )
}
```

**Status**: PENDING

---

### [✅] Test 8: Update configurePageRoutes signature
**File**: Update routing tests

**Test**: Routes should accept ValidationService from Context.

**Implementation**: Add `validationService: ValidationService` parameter to `configurePageRoutes()`.

**Status**: PENDING

---

## Notes
- This is a structural refactoring - all tests should continue passing
- Context pattern replaces ad-hoc dependency creation in Application.module()
- Makes it easy to create test contexts with fake dependencies
- Can add more dependency layers as needed (e.g., UseCases, Handlers)
- Consider adding resource cleanup (e.g., closing HTTP clients) via TopContext.close()

---
---

# TDD Plan: Component Organization - Header and Footer

## Feature Description
Extract reusable header and footer components from page templates. This improves code organization and demonstrates component patterns for the showcase repository.

## Implementation Approach
- Create `HeaderComponent.kt` with `FlowContent.headerComponent()` function
- Create `FooterComponent.kt` with `FlowContent.footerComponent()` function
- Update `MainTemplate` to use these components
- Support optional customization (e.g., active page highlighting)

## Benefits
- Better code organization
- Reusable components
- Demonstrates composition patterns
- Easier to maintain consistent layout
- Clear separation of concerns

---

## Tests (TDD Order)

### [ ] Test 1: HeaderComponent renders header element
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/HeaderComponentTest.kt`

**Test**: `headerComponent()` should render a `<header>` element with appropriate content and styling classes.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/pages/HeaderComponent.kt
package no.mikill.kotlin_htmx.pages

import kotlinx.html.*

fun FlowContent.headerComponent() {
    header(classes = "site-header") {
        h1 { +"Kotlin HTMX Demo" }
        nav {
            a(href = "/") { +"Home" }
            a(href = "/demo") { +"Demos" }
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 2: FooterComponent renders footer element
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FooterComponentTest.kt`

**Test**: `footerComponent()` should render a `<footer>` element with copyright and links.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/pages/FooterComponent.kt
package no.mikill.kotlin_htmx.pages

import kotlinx.html.*

fun FlowContent.footerComponent() {
    footer(classes = "site-footer") {
        div {
            +"A "
            a(href = "https://www.mikill.no") { +"Mikill Digital" }
            +" project"
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 3: Update MainTemplate to use components
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/PageTemplatesTest.kt`

**Test**: MainTemplate should render using headerComponent() and footerComponent().

**Implementation**:
```kotlin
// In PageTemplates.kt, update MainTemplate:
override fun HTML.apply() {
    head { /* ... */ }
    body {
        headerComponent()
        main {
            insert(content)
        }
        footerComponent()
    }
}
```

**Status**: PENDING

---

### [✅] Test 4: Header renders in full page
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ApplicationTest.kt`

**Test**: When rendering any page, the response should include the header HTML.

**Implementation**: Integration test - verify header appears in actual page renders.

**Status**: PENDING

---

### [✅] Test 5: Footer renders in full page
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ApplicationTest.kt`

**Test**: When rendering any page, the response should include the footer HTML.

**Implementation**: Integration test - verify footer appears in actual page renders.

**Status**: PENDING

---

## Notes
- This is primarily a structural refactoring
- All existing page tests should continue to pass
- CSS classes can be added/customized as needed
- Consider adding active page highlighting in navigation
- Header/footer should not appear in HTMX fragment responses (only in full page loads)

---
---

# TDD Plan: Routing Utilities - HtmlRenderUtils and UUID Extension

## Feature Description
Add utility functions for common routing patterns: rendering HTML fragments and extracting UUID parameters with proper error handling.

## Implementation Approach
- Create `HtmlRenderUtils` object with `respondHtmlFragment()` and `partialHtml()` functions
- Move existing fragment rendering to use this utility
- Add `Parameters.getUUID()` extension function with custom exception
- Add `MissingResourceBecauseInvalidUuidException` for better error messages

## Benefits
- Centralized HTML rendering logic
- Consistent error handling for UUID parameters
- Better error messages for debugging
- Reduces code duplication
- Shows Kotlin extension function patterns

---

## Tests (TDD Order)

### [ ] Test 1: HtmlRenderUtils.partialHtml renders body content only
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/HtmlRenderUtilsTest.kt`

**Test**: `partialHtml()` should render HTML content without `<html>` or `<body>` wrapper tags.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/pages/HtmlRenderUtils.kt
package no.mikill.kotlin_htmx.pages

import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.html.*
import kotlinx.html.consumers.filter
import kotlinx.html.stream.appendHTML

object HtmlRenderUtils {
    fun partialHtml(block: BODY.() -> Unit): String = buildString {
        appendHTML().filter {
            if (it.tagName in listOf("html", "body")) SKIP else PASS
        }.html {
            body {
                block(this)
            }
        }
    }.trim()
}
```

**Status**: PENDING

---

### [ ] Test 2: HtmlRenderUtils.respondHtmlFragment sends correct content type
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/HtmlRenderUtilsTest.kt`

**Test**: `respondHtmlFragment()` should respond with `text/html; charset=UTF-8` content type.

**Implementation**:
```kotlin
suspend fun ApplicationCall.respondHtmlFragment(
    status: HttpStatusCode = HttpStatusCode.OK,
    block: BODY.() -> Unit
) {
    val text = partialHtml(block)
    respond(TextContent(text, ContentType.Text.Html.withCharset(Charsets.UTF_8), status))
}
```

**Status**: PENDING

---

### [ ] Test 3: HtmlRenderUtils.respondHtmlFragment supports custom status
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/HtmlRenderUtilsTest.kt`

**Test**: Should be able to respond with custom HTTP status (e.g., 400 Bad Request).

**Implementation**: Already supported via parameter - verify with test.

**Status**: PENDING

---

### [✅] Test 4: Move existing fragment rendering to use HtmlRenderUtils
**File**: Update existing route handlers

**Test**: All routes currently using custom fragment rendering should use `call.respondHtmlFragment()`.

**Implementation**:
- Update imports in Routes.kt
- Replace manual fragment rendering with `call.respondHtmlFragment { }`
- Remove old respondHtmlFragment from HtmlElements.kt if it exists

**Status**: PENDING

---

### [✅] Test 5: Parameters.getUUID extracts valid UUID
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/routing/ParameterExtensionsTest.kt`

**Test**: Given valid UUID string in parameters, `getUUID("id")` should return UUID instance.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/routing/ParameterExtensions.kt
package no.mikill.kotlin_htmx.routing

import io.ktor.http.Parameters
import java.util.UUID

class MissingResourceBecauseInvalidUuidException(
    parameterName: String
) : IllegalArgumentException("Invalid UUID in parameter: $parameterName")

fun Parameters.getUUID(name: String): UUID {
    val value = this[name]
        ?: throw IllegalArgumentException("Parameter $name is required")
    return try {
        UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
        throw MissingResourceBecauseInvalidUuidException(name)
    }
}
```

**Status**: PENDING

---

### [✅] Test 6: Parameters.getUUID throws for missing parameter
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/routing/ParameterExtensionsTest.kt`

**Test**: When parameter is not present, should throw `IllegalArgumentException` with clear message.

**Implementation**: Already handled in Test 5 - verify with test.

**Status**: PENDING

---

### [✅] Test 7: Parameters.getUUID throws custom exception for invalid UUID
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/routing/ParameterExtensionsTest.kt`

**Test**: When parameter value is not a valid UUID, should throw `MissingResourceBecauseInvalidUuidException`.

**Implementation**: Already handled in Test 5 - verify with test.

**Status**: PENDING

---

### [✅] Test 8: Update existing routes to use Parameters.getUUID
**File**: Update Routes.kt

**Test**: All routes extracting UUID parameters should use `call.parameters.getUUID()`.

**Implementation**:
- Replace `UUID.fromString(call.parameters["id"]!!)` patterns
- Replace `call.parameters["id"]?.let { UUID.fromString(it) }` patterns
- Add proper error handling for MissingResourceBecauseInvalidUuidException

**Status**: PENDING

---

## Notes
- HtmlRenderUtils reduces duplication in HTMX fragment responses
- The UUID extension improves error messages for debugging
- Consider adding StatusPages configuration to handle MissingResourceBecauseInvalidUuidException globally
- These utilities make the codebase more maintainable and show Kotlin best practices
- Can add more parameter extraction helpers (e.g., getInt, getLocalDate) following same pattern

---

# Implementation Priority

**✅ COMPLETED FEATURES** (removed from plan above):
1. ValidationService - Type-safe validation with Jakarta Bean Validation
2. PropertyPath - Type-safe nested property references with getValue()
3. Person Registration - Nested Domain Models (Person, Address, AddressType, PersonRepository)
4. Person Registration Forms with PropertyPath - Complete registration flow with FormBuilderDsl

**⚠️ OPTIONAL ENHANCEMENTS** (remaining in plan above):
1. **HtmlConstraints** - PARTIALLY COMPLETED (@NotBlank/@Size/Email working, missing @Pattern/@NotEmpty/@NotNull)
2. **Type-Safe Form Components** - PARTIALLY COMPLETED (basic components done, FormBuilderDsl supersedes this)
3. **Context Pattern** - NOT STARTED (Dependency injection pattern for better testability)
4. **Routing Utilities** - PARTIALLY COMPLETED (some utilities exist, could add more)
5. **Component Organization** - NOT STARTED (Header/Footer extraction for better code organization)

## Summary

**✅ PRODUCTION READY**: The core Person/Address registration system is fully implemented with:
- Type-safe three-level property nesting (`person.addresses[0].streetAddress`)
- Advanced form DSL with automatic binding (`form{}` and `indexedForm{}`)
- Enum support with automatic dropdowns
- Complete validation with Jakarta Bean Validation
- Full test coverage

**Next Steps**: Pick any optional enhancement from the remaining plans above to continue development.
