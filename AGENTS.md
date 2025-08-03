# Project Guidelines

## Application functionality

This is a demo application for showcasing plain HTML, CSS, HTMX and Kotlin.

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

# Run the application. This starts the server and is a blocking operation.
./gradlew run

# Update the npm dependencies for postcss
cd src/main/resources/postcss && npm run build

# Check code formatting with ktlint
./gradlew ktlintCheck

# Auto-format code with ktlint
./gradlew ktlintFormat
```


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
    - Verify code formatting with `./gradlew ktlintCheck` before considering a task complete

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

6. **Task Management**
    - Always update improvement-tasks.md when completing tasks
    - Mark completed tasks with ✅ COMPLETED status
    - Strike through completed action items
    - Add status notes explaining what was accomplished
    - This provides clear audit trail of work done

7. **Operations**
    - When refactoring make sure to delete the old code
