# Kotlin-HTMX Features

> **⚠️ Warning:** This is a demo project, not a library or a production application. You should copy parts that are relevant to you and change it when you need. Good luck.

This document describes the features and technical capabilities of the kotlin-htmx project. The project serves as a showcase for building modern web applications using **Kotlin + Ktor + HTMX** with plain HTML and CSS, emphasizing type safety, server-side rendering, and minimal JavaScript.

## Table of Contents
- [User-Facing Features](#user-facing-features)
- [Technical Capabilities](#technical-capabilities)
- [Architecture Highlights](#architecture-highlights)
- [Advanced Patterns](#advanced-patterns)

---

## User-Facing Features

### 1. Real-Time Checkbox Synchronization (`/demo/htmx/checkboxes`)

A demonstration of real-time state synchronization across multiple browser windows using HTMX and Server-Sent Events (SSE).

**Features:**
- Synchronize checkbox states across multiple browser tabs/windows
- Display live count of connected browsers
- Support for thousands of checkboxes (configurable, default 5000)
- Infinite scrolling with batched loading for performance
- QR code for easy mobile testing

**Technical Implementation:**
- Server-Sent Events (SSE) for push notifications
- Batch updates to minimize network traffic
- In-memory state management with thread-safe collections
- HTMX `sse-swap` attribute for declarative DOM updates
- Dead connection detection and automatic cleanup
- Dynamic batch loading triggered by `revealed` HTMX event

**Blog Post:** [HTMX SSE: Easy updates of HTML state with no JavaScript](https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/)

---

### 2. Person Registration System (`/person/register`)

A complete multi-step registration form demonstrating complex domain modeling with nested validation.

**Features:**
- Two-step registration flow: basic person info, then add addresses
- Support for multiple addresses per person
- Type-safe form binding with nested properties
- Real-time validation with inline error messages
- HTML5 constraint validation derived from backend annotations
- Enum dropdowns for address types (HOME, WORK, OTHER)

**Technical Implementation:**
- Three-level property nesting: `person.addresses[0].streetAddress`
- Type-safe PropertyPath system with compile-time checking
- Jakarta Bean Validation with cascading validation (`@Valid`)
- Custom Form DSL for DRY form rendering
- Jackson-based form parameter binding
- Aggregate root pattern (Person is the root, Address is part of the aggregate)

---

### 3. Todo List Demos

Multiple implementations of the same feature using different technologies, all in one application.

#### Plain HTML (`/demo`)
- Server-rendered HTML with standard forms
- Full page reloads on interaction

#### HTMX Component (`/demo/htmx`)
- HTMX-powered partial updates
- No page reloads, just fragment replacement

#### Multi-Framework Page (`/demo/multi`)
- HTML, HTMX, React, and Lit components on the same page
- Demonstrates polyglot frontend architecture
- Shared JSON endpoint for data

---

### 4. Admin Demo (`/demo/admin`)
- Dynamic content loading with HTMX
- Cache control headers demonstration
- Fragment-based content updates

---

### 5. Questions Page (`/demo/htmx/questions`)
- Form submission with HTMX
- Fragment responses after form submission
- Validation and error handling

---

### 6. Selection Wizard (`/select`)
- Multi-step wizard flow
- Search functionality with external API integration
- HX-Boost for SPA-like navigation
- Server-side rendering with progressive enhancement

---

## Technical Capabilities

### 1. Type-Safe Form Handling

**PropertyPath System**
```kotlin
// Type-safe nested property references with compile-time checking
val path = Person::addresses.at(0, Address::streetAddress)
// Generates: "addresses[0].streetAddress"
```

**Benefits:**
- Compile-time safety: Renaming properties updates all references
- IDE support: Autocompletion and refactoring tools work
- No string literals for field names
- Supports simple, nested, and indexed properties

**Form DSL**
```kotlin
form(person, violations) {
    field(Person::firstName.toPath(), "First Name")
    field(Person::lastName.toPath(), "Last Name")
    field(Person::email.toPath(), "Email")
}

indexedForm(person, violations, Person::addresses, 0) {
    field(Address::streetAddress, "Street Address")
    enumSelect(Address::type, "Address Type")
}
```

---

### 2. Automatic HTML Constraint Generation

Validation annotations on domain models automatically generate HTML5 input attributes:

```kotlin
data class Person(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 50)
    val firstName: String,

    @field:Email(message = "Must be a valid email")
    val email: String
)
```

Automatically generates:
```html
<input name="firstName" required maxlength="50" />
<input name="email" type="email" required />
```

**Supported Annotations:**
- `@NotBlank`, `@NotEmpty`, `@NotNull` → `required`
- `@Size(max=N)` → `maxlength="N"`
- `@Size(min=N)` → `minlength="N"`
- `@Email` → `type="email"`
- `@Pattern(regexp="...")` → `pattern="..."`

---

### 3. Advanced Form Binding

**Jackson-Based Parameter Binding**

Converts HTTP form parameters to typed objects using Jackson:

```kotlin
// Simple binding
val person = call.receiveParameters().bindTo<Person>()

// Indexed property binding
val address = call.receiveParameters()
    .bindIndexedProperty<Address>("addresses", 0)
```

**Handles:**
- Nested properties: `address.street`
- Indexed arrays: `addresses[0].city`
- Complex object graphs
- Type conversion and validation

---

### 4. Server-Side CSS Processing

**PostCSS Integration with GraalJS**

Processes CSS/SCSS on-demand at runtime using PostCSS bundled with Webpack:

```kotlin
// Automatic processing of .scss files
GET /css/styles.css → processes styles.scss → returns CSS
```

**Features:**
- On-demand CSS processing (no build step required for development)
- PostCSS plugins: autoprefixer, nested syntax, etc.
- GraalJS context pooling for performance
- Webpack bundled PostCSS with all plugins
- Cache-friendly with appropriate headers

**Performance:**
- Context pool (2 contexts) to avoid slow initialization
- Synchronized access for thread safety
- Caching via HTTP headers

---

### 5. HTMX-Specific Ktor Support

**Declarative HTMX Routing**

```kotlin
// HTMX-specific routes
hx.post("/search") {
    // Only handles requests with HX-Request header
}

hx.get("/item/{id}") {
    // Returns HTML fragments, not full pages
}
```

**HtmlRenderUtils**
```kotlin
// Render HTML fragments for HTMX
call.respondHtmlFragment {
    div { +"Dynamic content" }
}

// Partial HTML without <html> or <body> tags
val html = partialHtml {
    section { +"Content" }
}
```

---

### 6. Server-Sent Events (SSE) with HTMX

**Real-Time Updates Without WebSockets**

```html
<!-- Client-side HTMX configuration -->
<div hx-ext="sse" sse-connect="/events">
    <span sse-swap="update-message">Initial content</span>
</div>
```

```kotlin
// Server-side SSE endpoint
sse("events") {
    send("New data", event = "update-message", id = uuid)
}
```

**Features:**
- Automatic reconnection with Last-Event-ID
- Bidirectional updates (SSE + HTMX requests)
- Connection lifecycle management
- Dead connection detection and cleanup
- Broadcast to multiple connected clients

---

### 7. External API Integration

**Type-Safe HTTP Client**

```kotlin
class LookupClient(private val apiKey: String) {
    suspend fun search(query: String): List<SearchResult> {
        val response = httpClient.get("https://api.example.com/search") {
            parameter("q", query)
            header("Authorization", "Bearer $apiKey")
        }
        return response.body()
    }
}
```

**Features:**
- Ktor HTTP Client with content negotiation
- Automatic JSON serialization/deserialization with Jackson
- Structured error handling
- Environment-based configuration

---

## Architecture Highlights

### 1. Domain-Driven Design

**Aggregate Root Pattern**
- Person is the aggregate root
- Address is part of the aggregate (never saved separately)
- All operations work on complete aggregates
- Validation cascades from root to children

**Repository Pattern**
```kotlin
class PersonRepository {
    fun save(person: Person): Person
    fun findById(id: UUID): Person?
    fun findAll(): List<Person>
}
```

**Note on Repository Implementation:** The PersonRepository in this demo is a **fake repository** using in-memory storage with `ConcurrentHashMap` for thread safety. This is the same pattern typically used for testing. In production, this would be replaced with a real database implementation (e.g., using Exposed, jOOQ, or direct JDBC). The interface remains the same, making it easy to swap implementations.

**Rich Domain Models**
- Business logic in domain objects
- Validation annotations on domain classes
- Immutable data classes
- No anemic domain models

---

### 2. Test-Driven Development (TDD)

The project follows strict TDD principles:
- Red → Green → Refactor cycle
- Comprehensive test coverage
- Object Mother pattern for test data
- Integration tests for complete flows

**Test Types:**
- Unit tests for domain logic
- Integration tests for routes
- Selenium tests for end-to-end flows
- Form binding tests
- Validation tests

**Test Tools:**
- JUnit 5
- AssertJ for fluent assertions
- Ktor TestApplication for route testing
- Selenium WebDriver for browser testing

**Object Mother Pattern for Test Data**

The project uses the Object Mother pattern via Kotlin extension functions to create valid test data:

```kotlin
// Extension functions on companion objects create valid instances
fun Person.Companion.valid() = Person(
    firstName = "John",
    lastName = "Doe",
    email = "john.doe@example.com",
    addresses = emptyList()
)

fun Address.Companion.valid() = Address(
    type = AddressType.HOME,
    streetAddress = "123 Main St",
    city = "Springfield",
    postalCode = "12345",
    country = "USA"
)

// Usage in tests - use .copy() for simple property changes
val validPerson = Person.valid()
val personWithCustomEmail = Person.valid().copy(email = "custom@example.com")
val personWithAddress = Person.valid().copy(
    addresses = listOf(Address.valid())
)

// Parameters are reserved for complex object graph construction
fun Order.Companion.valid(numberOfItems: Int = 2) = Order(
    customerId = UUID.randomUUID(),
    items = (1..numberOfItems).map { OrderItem.valid() }
)

val smallOrder = Order.valid(numberOfItems = 1)
val largeOrder = Order.valid(numberOfItems = 10)
```

**Benefits:**
- **DRY Testing**: Single source of truth for valid test data
- **Test Intent Clarity**: Using `.copy()` makes it immediately clear which properties matter for each specific test case
- **Idiomatic Kotlin**: Leverages data class `.copy()` for property overrides
- **Minimal Test Code**: Tests only specify values that differ from the valid defaults
- **Parameters for Complexity**: Reserve `.valid()` parameters for controlling object graph complexity (number of items, depth of nesting)
- **Composable**: Nest valid objects within each other
- **Refactoring Safe**: Changes to domain model automatically update all tests
- **Self-Documenting**: Shows what constitutes a valid object at a glance
- **No Test Data Brittleness**: Tests don't break when unrelated properties are added to domain models

---

### 3. Type Safety Throughout

**Kotlin Type System**
- Data classes for immutable DTOs
- Sealed classes for state representation
- Enum classes for constrained values
- Non-null by default
- Extension functions for DSL creation

**Reflection-Based Type Safety**
```kotlin
// Type-safe property references
Person::firstName
Address::type

// Compile-time checked paths
Person::addresses.at(0, Address::city)
```

---

### 4. Configuration Management

**Environment-Based Configuration**

```kotlin
data class ApplicationConfig(
    val lookupApiKey: String
)

// Loads from environment variables or .env files
val config = ApplicationConfig.load()
```

**Features:**
- Environment variables take precedence
- .env file support for local development
- Type-safe configuration objects
- Fail-fast on missing required config

---

### 5. Separation of Concerns

**Layered Architecture:**
1. **Routes**: HTTP handling, parameter extraction
2. **Pages**: HTML rendering, template logic
3. **Services**: Business logic, validation
4. **Repositories**: Data access
5. **Domain**: Core business objects
6. **Clients**: External service integration

**File Organization:**
```
src/main/kotlin/
├── Application.kt (entry point)
├── Routes.kt (route configuration)
├── plugins/ (Ktor plugins)
├── pages/ (HTML templates and components)
├── forms/ (form binding utilities)
├── validation/ (validation service)
├── registration/ (person registration domain)
└── integration/ (external clients)
```

---

## Advanced Patterns

### 1. Form Builder DSL

A Kotlin DSL for reducing form rendering boilerplate:

```kotlin
class FormBuilder<T>(
    private val valueObject: T,
    private val violations: Map<String, List<String>>
) {
    fun <R> FlowContent.field(
        propertyPath: PropertyPath<T, R>,
        label: String,
        inputType: InputType? = null
    ) {
        // Automatically extracts value, applies constraints,
        // and displays validation errors
    }
}
```

**Benefits:**
- DRY: Define field once, get value binding + validation + errors
- Type-safe: Property paths checked at compile time
- Extensible: Easy to add new field types
- Reusable: Works with any domain object

---

### 2. Property Value Extraction

Type-safe extraction of values from nested objects:

```kotlin
sealed class PropertyPath<T, R> {
    abstract fun getValue(obj: T): R?
}

// Extract nested value
val path = Person::addresses.at(0, Address::city)
val city = path.getValue(person) // Type: String?
```

**Implementation:**
- Direct properties: Use Kotlin reflection
- Nested properties: Chain getValue calls
- Indexed properties: Access list element by index

---

### 3. Validation Result Pattern

Functional approach to validation:

```kotlin
sealed class ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>()
    data class Invalid(
        val violations: Map<String, List<String>>
    ) : ValidationResult<Nothing>()
}

// Usage
when (val result = validationService.validate(person)) {
    is Valid -> repository.save(result.value)
    is Invalid -> renderFormWithErrors(result.violations)
}
```

---

### 4. HTML Fragment Utilities

Utilities for consistent HTML fragment rendering:

```kotlin
object HtmlRenderUtils {
    // Render without <html> or <body> tags
    fun partialHtml(block: BODY.() -> Unit): String

    // Respond with HTML fragment
    suspend fun ApplicationCall.respondHtmlFragment(
        status: HttpStatusCode = HttpStatusCode.OK,
        block: BODY.() -> Unit
    )
}
```

---

### 5. Component Organization

Reusable HTML components using kotlinx.html:

```kotlin
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

**Pattern Benefits:**
- Type-safe HTML generation
- Reusable components
- Composable architecture
- No template language needed

---

## Technology Stack Summary

### Backend
- **Kotlin** 2.2.20 on JVM 21
- **Ktor** 3.3.1 (web framework)
- **kotlinx.html** (type-safe HTML DSL)
- **Jackson** (JSON/form serialization)
- **Jakarta Bean Validation** (Hibernate Validator)
- **GraalJS** 25.0.0 (PostCSS processing)

### Frontend
- **HTMX** (hypermedia interactions)
- **Server-Sent Events** (real-time updates)
- **Plain HTML/CSS** (no build step required)
- **Tailwind CSS** potential via PostCSS
- **React/Lit** (optional, for comparison demos)

### Build & Tooling
- **Gradle** with Kotlin DSL
- **Shadow JAR** for deployment
- **ktlint** for code formatting
- **Mise** for version management

### Testing
- **JUnit 5** (test framework)
- **AssertJ** (fluent assertions)
- **Selenium** (browser automation)
- **Ktor Test Host** (integration testing)

---

## Key Takeaways for Developers

1. **Type Safety Everywhere**: From routes to forms to validation, Kotlin's type system prevents entire classes of errors

2. **Server-Side Rendering**: All HTML is generated on the server, reducing JavaScript complexity

3. **Progressive Enhancement**: Start with plain HTML forms, enhance with HTMX for better UX

4. **HTMX + SSE**: Real-time updates without WebSocket complexity

5. **Domain-Driven Design**: Rich domain models with validation, not anemic DTOs

6. **TDD Throughout**: Every feature test-driven from the start

7. **No Framework Lock-In**: Plain Kotlin + standard libraries, easy to migrate

8. **Production-Ready Patterns**: Configuration management, error handling, logging, compression

9. **Developer Experience**: Type safety, refactoring support, minimal boilerplate

10. **Educational Value**: Each demo showcases a different architectural pattern or technique

---

## Live Demo

The application is deployed and available at: [https://kotlin-htmx.fly.dev](https://kotlin-htmx.fly.dev)

## Source Code

Full source code with comprehensive test coverage available at:
[https://github.com/anderssv/kotlin-htmx/](https://github.com/anderssv/kotlin-htmx/)

---

## Related Resources

- [Blog: HTMX + SSE Implementation](https://blog.f12.no/wp/2024/11/11/htmx-sse-easy-updates-of-html-state-with-no-javascript/)
- [Mikill Digital](https://www.mikill.no)
- [Author's Blog](https://blog.f12.no)
