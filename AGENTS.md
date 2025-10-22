# Project Guidelines

## Application functionality

This is a demo application for showcasing plain HTML, CSS, HTMX and Kotlin.

## Development Workflow

**IMPORTANT**: Always follow the plan in `docs/plan.md` when working on tasks.

When the user says "go":
1. Find the next unmarked test in `docs/plan.md`
2. Implement the test following TDD principles
3. Implement only enough code to make that test pass
4. Mark the test as completed in `docs/plan.md`
5. Run all tests to ensure nothing is broken
6. Format and verify code with `./gradlew ktlintFormat` (this both formats and checks in one step)
7. **Update `docs/features.md`** when completing significant features (features.md is for developers learning techniques)

This ensures systematic, incremental progress through planned features.

**TDD Note**: Compile errors are NOT a proxy for failing tests. A test must compile and execute, then fail with a meaningful assertion error to be considered a proper "Red" phase in TDD. Add necessary dependencies and imports first to get a proper failing test.

## Tech Stack

- Kotlin on the JVM
- Gradle (Kotlin DSL) for build management
- JUnit and AssertJ for testing
- Jackson for JSON handling

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "fully.qualified.TestClassName"
```

## Tools

```bash
# Print new versions of dependencies
./gradlew dependencyUpdates

# Verify version upgrades by building and running the shadow jar
# Always run this after upgrading dependencies to ensure everything works
./gradlew shadowJar && java -jar build/libs/kotlin-htmx-all.jar

# Run the application. This starts the server and is a blocking operation.
./gradlew run

# Update the npm dependencies for postcss
cd src/main/resources/postcss && npm run build

# Format and check code with ktlint (recommended - does both formatting and checking)
./gradlew ktlintFormat

# Check code formatting with ktlint only (without auto-formatting)
./gradlew ktlintCheck
```

**Note**: Prefer `ktlintFormat` over `ktlintCheck` as it both formats the code and verifies it in one step.


## Development Setup

1. Install prerequisites:
    - Mise version manager
    - Java 21 (via ASDF)
    - Git

2. Build project:
   ```bash
   ./gradlew build
   ```

## Best Practices

1. **Testing**
    - Write tests for all new features
    - Write tests first (TDD approach)
    - Use fakes instead of mocks when possible
    - Follow Arrange-Act-Assert pattern
    - Use test data builders (usually objects with valid() methods, a variant of the Object Mother pattern)
    - Run all tests after finishing a task
    - Format and verify code with `./gradlew ktlintFormat` before considering a task complete (this both formats and checks)

    - **Testing Layers - Understanding the Differences**:
      - **Page/Component Tests (e.g., `PersonRegistrationPageTest`)**: Test HTML structure and form field generation in isolation. These verify that form inputs have correct `name` attributes, proper field structure, and correct HTML element nesting. They catch structural issues without HTTP overhead.
      - **Endpoint/Route Tests (e.g., `PersonRegistrationRoutesTest`)**: Test HTTP behavior, validation flow, redirects, and repository state changes. They verify HTML content with `contains()` checks for specific text, but do NOT verify form field structure or `name` attributes.
      - **E2E/Selenium Tests (e.g., `HtmxCheckboxPageTest`)**: Test full browser behavior, JavaScript execution, SSE connections, and multi-browser synchronization.

    - **Why All Three Layers Matter**:
      - Page tests catch form binding issues (wrong field names) that endpoint tests miss
      - Endpoint tests catch HTTP flow and validation issues that page tests miss
      - E2E tests catch browser behavior and JavaScript issues that neither catch
      - These are NOT duplicates - they test different concerns at different levels

    - **Skip low-value utility tests**: Don't write unit tests for simple utility functions (like `partialHtml()` or `respondHtmlFragment()`) if they are already exercised by integration/endpoint tests. Focus testing effort on complex logic, edge cases, and code that's hard to verify through integration tests alone. Simple wrappers and formatting utilities that are called by tested endpoints don't need dedicated unit tests.

2. **Code Organization**
    - Follow domain-driven package structure
    - Keep services focused and small
    - Use dependency injection for better testability
    - Maintain clear separation between domain and infrastructure code

3. **Code Principles**
    - Favour immutability
    - Use data classes for simple data structures
    - Avoid side effects in functions
    - Prefer composition to inheritance
    - Use sealed classes for representing state
    - Use UUIDs for unique identifiers
    - Prefer objects to primitive types
    - Re-use test data setup, prefer <class>.valid() test extension methods.
    - Use rich domain models, avoid splitting data into multiple tables unless necessary. JSONB column in PostgreSQL is a good option for complex data structures.
    - **Data Binding**: Prefer using Jackson for all serialization/deserialization. When working with formats that Jackson doesn't natively support (like HTML form parameters with dot notation), write utility functions to convert to an intermediate structure (Map, JsonNode) that Jackson can consume. Avoid writing custom deserializers; instead, adapt the input to Jackson's expectations.

4. **Naming Conventions**
    - *Domain.kt for domain models
    - *Repository.kt for data access
    - *Service.kt for business logic
    - *Fake.kt for test doubles
    - *Client.kt for clients to other services

5. **Documentation**
    - Check /doc directory for detailed guides
    - Keep README.md updated
    - Document complex business rules in code

6. **Documentation & Task Management**
    - **Always update `docs/plan.md`** when completing tests (mark with ✅ and status COMPLETED)
    - **Always update `docs/features.md`** when completing significant features
      - `features.md` is intended for developers wanting to learn the techniques in this repo
      - Focus on explaining patterns, approaches, and architectural decisions
      - Include code examples and explanations of "how" and "why"
    - Update improvement-tasks.md when completing tasks
    - Mark completed tasks with ✅ COMPLETED status
    - Add status notes explaining what was accomplished
    - This provides clear audit trail of work done

7. **Operations**
    - When refactoring make sure to delete the old code
