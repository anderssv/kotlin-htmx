# Project Guidelines

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
```

## Development Setup

1. Install prerequisites:
    - ASDF version manager
    - Java 21 (via ASDF)
    - Git

2. Build project:
   ```bash
   ./gradlew build
   ```

## Best Practices

1. **Testing**
    - Write tests first (TDD approach)
    - Use fakes instead of mocks when possible
    - Follow Arrange-Act-Assert pattern
    - Utilize test data builders
    - Run all tests after finishing a task

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

6. **Exercises specific instructions**
    - Follow the instructions in the exercise's README.md
    - Comments on the tests should be considered as correct, verify them against the test code
    - Use the provided test cases as a guide for implementation
    - Ensure all tests pass before submitting

7. **Operations**
    - When refactoring make sure to delete the old code
