# CURRENT FOCUS ⚡

**TOP PRIORITY: Address Form Example - Nested Property Validation**

The current focus is implementing a complete person registration system with nested addresses demonstrating three-level property nesting (`person.addresses[0].streetAddress`). This showcases:
- Type-safe nested property references with PropertyPath
- Collection validation with Jakarta Bean Validation
- Multi-form registration flow
- Aggregate root pattern

**Work on these features in order:**
1. **PropertyPath** - Type-safe nested property references (Section 7)
2. **Person Registration - Nested Domain Models** - Domain models with Address collection (Section 8)
3. **Person Registration Forms with PropertyPath** - Forms with nested address handling (Section 9)

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

### [ ] Test 4: Extract maxlength from @Size
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

### [ ] Test 5: Extract minlength from @Size
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

### [ ] Test 6: Extract both min and max from @Size
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given `@field:Size(min=3, max=100)`, should return both attributes.

**Implementation**: Combine both checks in Test 4 and 5.

**Status**: PENDING

---

### [ ] Test 7: Extract email type from @Email
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

### [ ] Test 8: Extract pattern from @Pattern
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

### [ ] Test 9: Combine multiple constraints
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/HtmlConstraintsTest.kt`

**Test**: Given a field with multiple annotations (`@NotBlank @Size(max=50) @Email`), should return all corresponding HTML attributes.

**Implementation**: Already handled by combining all previous checks - verify with test.

**Status**: PENDING

---

### [ ] Test 10: Handle property with no annotations
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

### [ ] Test 4: validatedTextInput allows custom configuration
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: The `configure` lambda should allow adding custom attributes (e.g., HTMX attributes, autocomplete, etc.)

**Implementation**: Call `configure()` at end of input block - already in Test 1.

**Status**: PENDING

---

### [ ] Test 5: validatedInputWithErrors renders input
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

### [ ] Test 6: validatedInputWithErrors renders single error
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

### [ ] Test 7: validatedInputWithErrors renders multiple errors
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: When a property has multiple violations, should render multiple error divs.

**Implementation**: Already handled by forEach in Test 6 - verify with test.

**Status**: PENDING

---

### [ ] Test 8: validatedInputWithErrors renders no errors for valid field
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/FormComponentsTest.kt`

**Test**: When violations map doesn't contain the property, should render no error divs.

**Implementation**: Already handled by safe navigation in Test 6 - verify with test.

**Status**: PENDING

---

### [ ] Test 9: Integration test with Person data class
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

### [ ] Test 4: Context.wireServices creates ValidationService
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

### [ ] Test 5: Context supports custom clock for testing
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: When passing a custom Clock to wireContext(), the TopContext should contain that clock.

**Implementation**: Already supported via default parameter - verify with test.

**Status**: PENDING

---

### [ ] Test 6: Context supports custom config for testing
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ContextTest.kt`

**Test**: When passing a custom ApplicationConfig, the TopContext should use it.

**Implementation**: Already supported via default parameter - verify with test.

**Status**: PENDING

---

### [ ] Test 7: Update Application.module to use Context
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

### [ ] Test 8: Update configurePageRoutes signature
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

### [ ] Test 4: Header renders in full page
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/ApplicationTest.kt`

**Test**: When rendering any page, the response should include the header HTML.

**Implementation**: Integration test - verify header appears in actual page renders.

**Status**: PENDING

---

### [ ] Test 5: Footer renders in full page
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

### [ ] Test 4: Move existing fragment rendering to use HtmlRenderUtils
**File**: Update existing route handlers

**Test**: All routes currently using custom fragment rendering should use `call.respondHtmlFragment()`.

**Implementation**:
- Update imports in Routes.kt
- Replace manual fragment rendering with `call.respondHtmlFragment { }`
- Remove old respondHtmlFragment from HtmlElements.kt if it exists

**Status**: PENDING

---

### [ ] Test 5: Parameters.getUUID extracts valid UUID
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

### [ ] Test 6: Parameters.getUUID throws for missing parameter
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/routing/ParameterExtensionsTest.kt`

**Test**: When parameter is not present, should throw `IllegalArgumentException` with clear message.

**Implementation**: Already handled in Test 5 - verify with test.

**Status**: PENDING

---

### [ ] Test 7: Parameters.getUUID throws custom exception for invalid UUID
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/routing/ParameterExtensionsTest.kt`

**Test**: When parameter value is not a valid UUID, should throw `MissingResourceBecauseInvalidUuidException`.

**Implementation**: Already handled in Test 5 - verify with test.

**Status**: PENDING

---

### [ ] Test 8: Update existing routes to use Parameters.getUUID
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
---

# TDD Plan: PropertyPath - Type-Safe Nested Property References

## Feature Description
Create a type-safe way to reference nested properties including indexed collections. This enables compile-time checking for paths like `person.addresses[1].streetAddress` while generating correct validation path strings.

## Implementation Approach
- Create `PropertyPath<T, R>` sealed class with variants for Direct, Nested, and Indexed properties
- Add extension functions for fluent API: `toPath()`, `then()`, `at()`
- Ensure path string matches Jakarta validation output format
- Extract HTML constraints from the final property in the chain

## Benefits
- Compile-time type safety for nested properties
- Refactoring support across all property references
- Clear API for working with collections
- Matches validation path format exactly
- Shows advanced Kotlin type system usage

---

## Tests (TDD Order)

### [✅] Test 1: PropertyPath.Direct represents simple property
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/PropertyPathTest.kt`

**Test**: `PropertyPath.Direct` should hold a `KProperty1` and generate correct path string.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/validation/PropertyPath.kt
package no.mikill.kotlin_htmx.validation

import kotlin.reflect.KProperty1

sealed class PropertyPath<T, R> {
    abstract val path: String

    data class Direct<T, R>(
        val property: KProperty1<T, R>
    ) : PropertyPath<T, R>() {
        override val path: String = property.name
    }
}
```

**Status**: COMPLETED

---

### [✅] Test 2: PropertyPath.Nested chains two properties
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/PropertyPathTest.kt`

**Test**: `PropertyPath.Nested` should combine parent and child paths with a dot.

**Implementation**:
```kotlin
data class Nested<T, M, R>(
    val parent: PropertyPath<T, M>,
    val property: KProperty1<M, R>
) : PropertyPath<T, R>() {
    override val path: String = "${parent.path}.${property.name}"
}
```

**Status**: COMPLETED

---

### [✅] Test 3: PropertyPath.Indexed handles collection element
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/PropertyPathTest.kt`

**Test**: `PropertyPath.Indexed` should generate path like `addresses[1].city`.

**Implementation**:
```kotlin
data class Indexed<T, E, R>(
    val listProperty: KProperty1<T, List<E>>,
    val index: Int,
    val elementProperty: KProperty1<E, R>
) : PropertyPath<T, R>() {
    override val path: String = "${listProperty.name}[$index].${elementProperty.name}"
}
```

**Status**: COMPLETED

---

### [✅] Test 4: KProperty1.toPath() extension creates Direct path
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/PropertyPathTest.kt`

**Test**: Calling `SomeClass::property.toPath()` should create `PropertyPath.Direct`.

**Implementation**:
```kotlin
fun <T, R> KProperty1<T, R>.toPath(): PropertyPath<T, R> =
    PropertyPath.Direct(this)
```

**Status**: COMPLETED

---

### [✅] Test 5: PropertyPath.then() chains nested properties
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/PropertyPathTest.kt`

**Test**: Calling `parent.then(Child::property)` should create `PropertyPath.Nested`.

**Implementation**:
```kotlin
fun <T, M, R> PropertyPath<T, M>.then(property: KProperty1<M, R>): PropertyPath<T, R> =
    PropertyPath.Nested(this, property)
```

**Status**: COMPLETED

---

### [✅] Test 6: KProperty1.at() creates indexed path
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/PropertyPathTest.kt`

**Test**: Calling `Person::addresses.at(0, Address::city)` should create `PropertyPath.Indexed` with path "addresses[0].city".

**Implementation**:
```kotlin
fun <T, E, R> KProperty1<T, List<E>>.at(
    index: Int,
    property: KProperty1<E, R>
): PropertyPath<T, R> =
    PropertyPath.Indexed(this, index, property)
```

**Status**: COMPLETED

---

### [✅] Test 7: PropertyPath works with real domain classes
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/validation/PropertyPathTest.kt`

**Test**: Create test Person and Address classes, verify all path types work correctly.

**Implementation**: Integration test with actual data classes.

**Status**: COMPLETED

---

## Notes
- PropertyPath maintains full type information throughout the chain
- The `path` property generates strings matching Jakarta validation format
- Index is runtime value (unavoidable for forms)
- Can be extended for more complex scenarios (nested indexed, etc.)
- Works seamlessly with existing HtmlConstraints

---
---

# TDD Plan: Person Registration - Nested Domain Models

## Feature Description
Create a complete person registration system demonstrating three-level nesting: `person.addresses[0].streetAddress`. This shows aggregate root pattern, collection validation, and type-safe form handling.

## Implementation Approach
- Create Person and Address domain models with full validation annotations
- Create AddressType enum
- Create PersonRepository for in-memory storage
- Build two-form registration flow: person details then addresses
- Use PropertyPath for all form fields
- Validate complete aggregate on save

## Benefits
- Real-world domain model example
- Demonstrates aggregate root pattern
- Shows collection validation with @Valid
- Three-level property nesting showcase
- Type-safe form handling at all levels

---

## Tests (TDD Order)

### [✅] Test 1: AddressType enum
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/AddressTypeTest.kt`

**Test**: `AddressType` enum should have HOME, WORK, OTHER values.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/registration/AddressType.kt
package no.mikill.kotlin_htmx.registration

enum class AddressType {
    HOME, WORK, OTHER
}
```

**Status**: PENDING

---

### [ ] Test 2: Address data class with validation
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/AddressTest.kt`

**Test**: `Address` should have id, type, streetAddress, city, postalCode, country with appropriate validation annotations.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/registration/RegistrationDomain.kt
package no.mikill.kotlin_htmx.registration

import jakarta.validation.constraints.*
import java.util.UUID

data class Address(
    val id: UUID = UUID.randomUUID(),
    @field:NotNull(message = "Address type is required")
    val type: AddressType?,
    @field:NotBlank(message = "Street address is required")
    @field:Size(max = 100, message = "Street address must be 100 characters or less")
    val streetAddress: String,
    @field:NotBlank(message = "City is required")
    @field:Size(max = 50, message = "City must be 50 characters or less")
    val city: String,
    @field:NotBlank(message = "Postal code is required")
    @field:Pattern(regexp = "[0-9]{4,5}", message = "Postal code must be 4-5 digits")
    val postalCode: String,
    @field:NotBlank(message = "Country is required")
    @field:Size(max = 50, message = "Country must be 50 characters or less")
    val country: String
)
```

**Status**: PENDING

---

### [ ] Test 3: Address validation with ValidationService
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/AddressTest.kt`

**Test**: Validate Address with ValidationService - verify violations for invalid fields.

**Implementation**: Use existing ValidationService to test Address validation rules.

**Status**: PENDING

---

### [ ] Test 4: Person data class with nested Address validation
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonTest.kt`

**Test**: `Person` should have id, firstName, lastName, email, addresses with @Valid annotation on addresses list.

**Implementation**:
```kotlin
data class Person(
    val id: UUID = UUID.randomUUID(),
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 50, message = "First name must be 50 characters or less")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 50, message = "Last name must be 50 characters or less")
    val lastName: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Must be a valid email address")
    val email: String,
    @field:Valid
    @field:Size(min = 1, message = "At least one address is required")
    val addresses: List<Address> = emptyList()
)
```

**Status**: PENDING

---

### [ ] Test 5: Person validates nested Address violations
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonTest.kt`

**Test**: When validating Person with invalid Address, violations should include paths like "addresses[0].city".

**Implementation**: Create Person with invalid Address, validate with ValidationService, verify violation paths.

**Status**: PENDING

---

### [ ] Test 6: Person requires at least one address
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonTest.kt`

**Test**: Person with empty addresses list should have violation on "addresses" property.

**Implementation**: Verify @Size(min=1) validation on addresses list.

**Status**: PENDING

---

### [ ] Test 7: Person.valid() test data builder
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonTest.kt`

**Test**: Create `Person.Companion.valid()` extension that returns valid Person with one valid Address.

**Implementation**:
```kotlin
fun Person.Companion.valid(
    firstName: String = "John",
    lastName: String = "Doe",
    email: String = "john.doe@example.com",
    addresses: List<Address> = listOf(Address.valid())
) = Person(
    firstName = firstName,
    lastName = lastName,
    email = email,
    addresses = addresses
)

fun Address.Companion.valid(
    type: AddressType = AddressType.HOME,
    streetAddress: String = "123 Main St",
    city: String = "Springfield",
    postalCode: String = "12345",
    country: String = "USA"
) = Address(
    type = type,
    streetAddress = streetAddress,
    city = city,
    postalCode = postalCode,
    country = country
)
```

**Status**: PENDING

---

### [ ] Test 8: PersonRepository stores and retrieves Person
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonRepositoryTest.kt`

**Test**: `PersonRepository.save()` stores Person, `findById()` retrieves it.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/registration/PersonRepository.kt
package no.mikill.kotlin_htmx.registration

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PersonRepository {
    private val persons = ConcurrentHashMap<UUID, Person>()

    fun save(person: Person): Person {
        persons[person.id] = person
        return person
    }

    fun findById(id: UUID): Person? = persons[id]
}
```

**Status**: PENDING

---

### [ ] Test 9: PersonRepository.update() modifies existing Person
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonRepositoryTest.kt`

**Test**: Can update Person (e.g., add addresses) and retrieve updated version.

**Implementation**: Update method replaces person in map.

**Status**: PENDING

---

### [ ] Test 10: PersonRepository.findAll() returns all persons
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonRepositoryTest.kt`

**Test**: `findAll()` should return list of all stored persons.

**Implementation**:
```kotlin
fun findAll(): List<Person> = persons.values.toList()
```

**Status**: PENDING

---

## Notes
- Person is the aggregate root - never save Address separately
- All operations work on complete Person object
- Repository is in-memory for demo purposes
- Validation cascades from Person to Addresses via @Valid
- Test data builders make tests more readable

---
---

# TDD Plan: Person Registration Forms with PropertyPath

## Feature Description
Create form components and pages that use PropertyPath for type-safe rendering of nested properties. Implement two-form registration flow: person details first, then add addresses iteratively.

## Implementation Approach
- Update form components to accept PropertyPath
- Create person registration page (first form)
- Create add address page (second form, can be used multiple times)
- Create person display page
- Use PropertyPath throughout for type safety

## Benefits
- Type-safe form rendering at all nesting levels
- Compile-time checking for all property references
- Clean separation of concerns
- Demonstrates complete registration flow
- Shows aggregate root pattern in action

---

## Tests (TDD Order)

### [ ] Test 1: validatedInput accepts PropertyPath
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/PropertyPathFormComponentsTest.kt`

**Test**: Create new `validatedInput()` that accepts `PropertyPath<T, R>` and renders input with path as name.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/pages/PropertyPathFormComponents.kt
package no.mikill.kotlin_htmx.pages

import kotlinx.html.*
import no.mikill.kotlin_htmx.validation.PropertyPath
import no.mikill.kotlin_htmx.validation.HtmlConstraints

fun <T, R> DIV.validatedInput(
    propertyPath: PropertyPath<T, R>,
    value: String,
    label: String,
    inputType: InputType = InputType.text,
    placeholder: String? = null
) {
    val pathString = propertyPath.path

    label {
        +label
        input(type = inputType, name = pathString) {
            this.value = value
            placeholder?.let { this.placeholder = it }
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 2: validatedInput extracts HTML constraints from PropertyPath
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/PropertyPathFormComponentsTest.kt`

**Test**: Input should have HTML attributes from validation annotations on the property.

**Implementation**:
```kotlin
// Extract property for HtmlConstraints based on PropertyPath type
val property = when (propertyPath) {
    is PropertyPath.Direct -> propertyPath.property
    is PropertyPath.Nested -> propertyPath.property
    is PropertyPath.Indexed -> propertyPath.elementProperty
}

val constraints = HtmlConstraints.getAttributes(property)

// Apply constraints to input element
constraints.forEach { (attrName, attrValue) ->
    when (attrName) {
        "required" -> required = true
        "maxlength" -> maxLength = attrValue
        "minlength" -> minLength = attrValue
        "pattern" -> pattern = attrValue
    }
}
```

**Status**: PENDING

---

### [ ] Test 3: validatedInputWithErrors displays violations by path
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/PropertyPathFormComponentsTest.kt`

**Test**: Add version that shows error messages from violations map using path string.

**Implementation**:
```kotlin
fun <T, R> DIV.validatedInputWithErrors(
    propertyPath: PropertyPath<T, R>,
    value: String,
    violations: Map<String, List<String>>,
    label: String,
    inputType: InputType = InputType.text,
    placeholder: String? = null
) {
    validatedInput(propertyPath, value, label, inputType, placeholder)

    violations[propertyPath.path]?.forEach { error ->
        div(classes = "error-message") {
            +error
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 4: Test with nested PropertyPath
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/pages/PropertyPathFormComponentsTest.kt`

**Test**: Create test with `Person::addresses.at(0, Address::city)` - verify name="addresses[0].city" and correct constraints.

**Implementation**: Integration test verifying PropertyPath works with real nested properties.

**Status**: PENDING

---

### [ ] Test 5: PersonRegistrationPage renders person form
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonRegistrationPageTest.kt`

**Test**: First form should render inputs for firstName, lastName, email using PropertyPath.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/registration/PersonRegistrationPage.kt
package no.mikill.kotlin_htmx.registration

import io.ktor.server.html.respondHtmlTemplate
import io.ktor.server.routing.RoutingContext
import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.*
import no.mikill.kotlin_htmx.validation.toPath

class PersonRegistrationPage {
    suspend fun renderPersonForm(
        context: RoutingContext,
        person: Person = Person(firstName = "", lastName = "", email = ""),
        violations: Map<String, List<String>> = emptyMap()
    ) {
        context.call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Register Person")) {
            mainSectionTemplate {
                emptyContentWrapper {
                    h1 { +"Register Person" }
                    form(method = FormMethod.post, action = "/person/register") {
                        div {
                            validatedInputWithErrors(
                                Person::firstName.toPath(),
                                person.firstName,
                                violations,
                                "First Name"
                            )
                        }
                        div {
                            validatedInputWithErrors(
                                Person::lastName.toPath(),
                                person.lastName,
                                violations,
                                "Last Name"
                            )
                        }
                        div {
                            validatedInputWithErrors(
                                Person::email.toPath(),
                                person.email,
                                violations,
                                "Email",
                                InputType.email
                            )
                        }
                        submitInput { value = "Continue to Addresses" }
                    }
                }
            }
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 6: POST /person/register validates and saves Person
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonRegistrationRoutesTest.kt`

**Test**: Posting valid person data should create Person with empty addresses, save to repository, redirect to add address page.

**Implementation**:
```kotlin
// In Routes.kt or PersonRegistrationRoutes.kt
post("/person/register") {
    val firstName = call.parameters["firstName"] ?: ""
    val lastName = call.parameters["lastName"] ?: ""
    val email = call.parameters["email"] ?: ""

    val person = Person(
        firstName = firstName,
        lastName = lastName,
        email = email,
        addresses = emptyList()
    )

    when (val result = validationService.validate(person)) {
        is ValidationResult.Valid -> {
            personRepository.save(result.value)
            call.respondRedirect("/person/${result.value.id}/address/add")
        }
        is ValidationResult.Invalid -> {
            personRegistrationPage.renderPersonForm(this, person, result.violations)
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 7: POST /person/register shows violations on invalid data
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonRegistrationRoutesTest.kt`

**Test**: Invalid person data should re-render form with error messages.

**Implementation**: Already handled in Test 6 - verify with test.

**Status**: PENDING

---

### [ ] Test 8: AddAddressPage renders address form with person context
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/AddAddressPageTest.kt`

**Test**: Address form should show person name (read-only), list existing addresses, and form for new address using PropertyPath with index.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/registration/AddAddressPage.kt
package no.mikill.kotlin_htmx.registration

import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.*
import no.mikill.kotlin_htmx.validation.at

class AddAddressPage {
    suspend fun renderAddAddressForm(
        context: RoutingContext,
        person: Person,
        newAddress: Address = Address(type = null, streetAddress = "", city = "", postalCode = "", country = ""),
        violations: Map<String, List<String>> = emptyMap()
    ) {
        val nextIndex = person.addresses.size

        context.call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Add Address")) {
            mainSectionTemplate {
                emptyContentWrapper {
                    h1 { +"Add Address for ${person.firstName} ${person.lastName}" }

                    if (person.addresses.isNotEmpty()) {
                        h2 { +"Existing Addresses" }
                        ul {
                            person.addresses.forEach { address ->
                                li {
                                    +"${address.type}: ${address.streetAddress}, ${address.city}"
                                }
                            }
                        }
                    }

                    h2 { +"New Address" }
                    form(method = FormMethod.post, action = "/person/${person.id}/address/add") {
                        div {
                            label {
                                +"Address Type"
                                select {
                                    name = Person::addresses.at(nextIndex, Address::type).path
                                    AddressType.values().forEach { type ->
                                        option {
                                            value = type.name
                                            +type.name
                                            if (type == newAddress.type) selected = true
                                        }
                                    }
                                }
                            }
                        }
                        div {
                            validatedInputWithErrors(
                                Person::addresses.at(nextIndex, Address::streetAddress),
                                newAddress.streetAddress,
                                violations,
                                "Street Address"
                            )
                        }
                        div {
                            validatedInputWithErrors(
                                Person::addresses.at(nextIndex, Address::city),
                                newAddress.city,
                                violations,
                                "City"
                            )
                        }
                        div {
                            validatedInputWithErrors(
                                Person::addresses.at(nextIndex, Address::postalCode),
                                newAddress.postalCode,
                                violations,
                                "Postal Code"
                            )
                        }
                        div {
                            validatedInputWithErrors(
                                Person::addresses.at(nextIndex, Address::country),
                                newAddress.country,
                                violations,
                                "Country"
                            )
                        }
                        submitInput { value = "Add Address" }
                    }

                    if (person.addresses.isNotEmpty()) {
                        form(method = FormMethod.post, action = "/person/${person.id}/complete") {
                            submitInput { value = "Complete Registration" }
                        }
                    }
                }
            }
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 9: POST /person/{id}/address/add validates and adds address
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/AddAddressRoutesTest.kt`

**Test**: Valid address data should be added to person, person re-saved, redirects back to add address page.

**Implementation**:
```kotlin
post("/person/{id}/address/add") {
    val personId = call.parameters.getUUID("id")
    val person = personRepository.findById(personId) ?: throw NotFoundException()

    val nextIndex = person.addresses.size
    val typeParam = call.parameters["addresses[$nextIndex].type"]
    val addressType = typeParam?.let { AddressType.valueOf(it) }

    val newAddress = Address(
        type = addressType,
        streetAddress = call.parameters["addresses[$nextIndex].streetAddress"] ?: "",
        city = call.parameters["addresses[$nextIndex].city"] ?: "",
        postalCode = call.parameters["addresses[$nextIndex].postalCode"] ?: "",
        country = call.parameters["addresses[$nextIndex].country"] ?: ""
    )

    // Validate the address
    when (val addressResult = validationService.validate(newAddress)) {
        is ValidationResult.Invalid -> {
            // Re-map violations to include index
            val remappedViolations = addressResult.violations.mapKeys { (key, _) ->
                "addresses[$nextIndex].$key"
            }
            addAddressPage.renderAddAddressForm(this, person, newAddress, remappedViolations)
        }
        is ValidationResult.Valid -> {
            val updatedPerson = person.copy(addresses = person.addresses + addressResult.value)
            personRepository.save(updatedPerson)
            call.respondRedirect("/person/${person.id}/address/add")
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 10: POST /person/{id}/address/add shows violations for invalid address
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/AddAddressRoutesTest.kt`

**Test**: Invalid address should re-render form with errors at correct paths (e.g., "addresses[0].city").

**Implementation**: Already handled in Test 9 - verify with test.

**Status**: PENDING

---

### [ ] Test 11: POST /person/{id}/complete validates full Person
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/CompleteRegistrationRoutesTest.kt`

**Test**: Completing registration should validate entire Person (including @Size(min=1) on addresses), redirect to person view if valid.

**Implementation**:
```kotlin
post("/person/{id}/complete") {
    val personId = call.parameters.getUUID("id")
    val person = personRepository.findById(personId) ?: throw NotFoundException()

    when (val result = validationService.validate(person)) {
        is ValidationResult.Invalid -> {
            // Show error - need at least one address
            addAddressPage.renderAddAddressForm(
                this,
                person,
                violations = result.violations
            )
        }
        is ValidationResult.Valid -> {
            call.respondRedirect("/person/${person.id}")
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 12: ViewPersonPage displays complete person with all addresses
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/ViewPersonPageTest.kt`

**Test**: Person view page should display all person details and all addresses.

**Implementation**:
```kotlin
// File: src/main/kotlin/no/mikill/kotlin_htmx/registration/ViewPersonPage.kt
package no.mikill.kotlin_htmx.registration

import kotlinx.html.*
import no.mikill.kotlin_htmx.pages.*

class ViewPersonPage {
    suspend fun renderPerson(
        context: RoutingContext,
        person: Person
    ) {
        context.call.respondHtmlTemplate(MainTemplate(template = EmptyTemplate(), "Person Details")) {
            mainSectionTemplate {
                emptyContentWrapper {
                    h1 { +"Person Details" }
                    dl {
                        dt { +"Name" }
                        dd { +"${person.firstName} ${person.lastName}" }
                        dt { +"Email" }
                        dd { +person.email }
                    }

                    h2 { +"Addresses" }
                    person.addresses.forEachIndexed { index, address ->
                        section {
                            h3 { +"Address ${index + 1}: ${address.type}" }
                            dl {
                                dt { +"Street" }
                                dd { +address.streetAddress }
                                dt { +"City" }
                                dd { +address.city }
                                dt { +"Postal Code" }
                                dd { +address.postalCode }
                                dt { +"Country" }
                                dd { +address.country }
                            }
                        }
                    }

                    a(href = "/person/${person.id}/address/add") {
                        +"Add Another Address"
                    }
                }
            }
        }
    }
}
```

**Status**: PENDING

---

### [ ] Test 13: GET /person/{id} displays person
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/ViewPersonRoutesTest.kt`

**Test**: GET request should load person from repository and render view page.

**Implementation**:
```kotlin
get("/person/{id}") {
    val personId = call.parameters.getUUID("id")
    val person = personRepository.findById(personId) ?: throw NotFoundException()
    viewPersonPage.renderPerson(this, person)
}
```

**Status**: PENDING

---

### [ ] Test 14: Integration test - complete registration flow
**File**: `src/test/kotlin/no/mikill/kotlin_htmx/registration/PersonRegistrationIntegrationTest.kt`

**Test**: Full flow test:
1. POST person registration → creates person
2. GET add address page → shows form
3. POST first address → adds address
4. POST second address → adds another address
5. POST complete → validates full person
6. GET person view → shows all data

**Implementation**: End-to-end integration test with test client.

**Status**: PENDING

---

## Notes
- PropertyPath provides type safety for all form fields
- Index is calculated dynamically (person.addresses.size)
- Aggregate root pattern: Always work with complete Person
- Validation happens at two levels: individual Address and complete Person
- Three-level nesting: person.addresses[1].streetAddress
- Can add more addresses iteratively before completing registration
- Form component reuse across all property types

---
---

# Implementation Priority

The plans above should be implemented in this order:

1. **ValidationService** ✅ COMPLETED
2. **PropertyPath** (Foundation for nested properties) ⚡ TOP PRIORITY
3. **Person Registration - Nested Domain Models** (Domain models with addresses) ⚡ TOP PRIORITY
4. **Person Registration Forms with PropertyPath** (Forms with nested address handling) ⚡ TOP PRIORITY
5. **HtmlConstraints** (Depends on ValidationService)
6. **Type-Safe Form Components** (Depends on HtmlConstraints)
7. **Context Pattern** (Independent structural improvement)
8. **Routing Utilities** (Independent utilities)
9. **Component Organization** (Independent structural improvement)

**Note**: The address-related features (PropertyPath, Person Registration models and forms) have been prioritized to TOP PRIORITY status.

Each feature is designed to work independently, but they combine to show a complete architectural pattern.
