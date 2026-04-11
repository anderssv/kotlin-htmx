# Project Improvement Tasks

## High Priority

### 1. Improve Error Handling
- **Issue**: Missing comprehensive error handling in configuration loading
- **Impact**: Application may fail silently or with poor error messages
- **Action**: Add proper exception handling in `ApplicationConfig.load()` and other critical paths
- **Files**: `src/main/kotlin/no/mikill/kotlin_htmx/Application.kt`

### 2. Security Improvements
- **Issue**: API keys stored in plain text files, no input validation
- **Impact**: Security vulnerabilities
- **Action**: Implement proper secret management, add input validation for all endpoints
- **Files**: Application-wide

## Medium Priority

### 3. Dependency Updates ✅ COMPLETED
- **Issue**: Several dependencies have newer versions available
- **Impact**: Missing bug fixes and security patches
- **Action**: ~~Update ktlint (1.5.0 → 1.7.1), kotlin-logging (5.1.0 → 7.0.11), ktlint gradle plugin (12.1.2 → 13.0.0)~~ **DONE**
- **Files**: `build.gradle.kts`, `gradle.properties`
- **Status**: Latest round: Kotlin 2.3.10→2.3.20, Ktor 3.4.0→3.4.2, ktlint plugin 14.0.1→14.2.0, Selenium 4.40.0→4.43.0. JUnit 6.x skipped (major version bump). Auto-reload enabled with suspend module function and ktor development block.

### 4. Test Coverage Improvements
- **Issue**: Limited unit test coverage (8 test files vs 24 source files)
- **Impact**: Reduced confidence in code changes
- **Action**: Add unit tests for domain logic, services, and utilities
- **Files**: Create new test files in `src/test/kotlin/`

### 5. Code Organization
- **Issue**: Mixed concerns in Application.kt, large functions
- **Impact**: Reduced maintainability
- **Action**: Extract configuration loading, refactor large functions, improve separation of concerns
- **Files**: `src/main/kotlin/no/mikill/kotlin_htmx/Application.kt`

### 6. Performance Optimizations
- **Issue**: Large number of checkboxes (5000) may impact performance
- **Impact**: Poor user experience with high loads
- **Action**: Implement pagination, lazy loading, or virtualization for checkbox demo
- **Files**: HTMX checkbox pages

### 7. Documentation
- **Issue**: Limited inline documentation and API documentation
- **Impact**: Reduced developer productivity
- **Action**: Add KDoc comments, improve README with setup instructions
- **Files**: All source files, README.md

## Low Priority

### 8. Build Improvements
- **Issue**: Could benefit from build optimizations
- **Impact**: Slower development cycle
- **Action**: Consider parallel test execution, build caching improvements
- **Files**: `build.gradle.kts`

### 9. Logging Improvements
- **Issue**: Inconsistent logging patterns
- **Impact**: Harder to debug issues
- **Action**: Standardize logging format, add structured logging
- **Files**: Application-wide

### 10. Environment Configuration
- **Issue**: Environment file handling could be more robust
- **Impact**: Configuration errors in different environments
- **Action**: Add validation for environment variables, better default handling
- **Files**: `src/main/kotlin/no/mikill/kotlin_htmx/Application.kt`

## Technical Debt

### Code Quality Issues
- Manual dependency injection commented as needing a proper solution
- Hard-coded values (port 8080, NUMBER_OF_BOXES environment variable)
- Mixed abstraction levels in some functions
- Some functions are too large and handle multiple responsibilities

### Architecture Improvements
- Consider implementing proper dependency injection framework
- Separate configuration from application startup
- Extract page routing logic to separate configuration class
- Consider implementing proper error handling middleware

## Testing Strategy Improvements
- Add integration tests for critical user journeys
- Implement API contract tests
- Add performance tests for high-load scenarios
- Consider property-based testing for domain logic

## Monitoring and Observability
- Add application metrics
- Implement health check endpoints
- Add request tracing
- Consider structured logging with correlation IDs