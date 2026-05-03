# Project Improvement Tasks

## High Priority

1. **Break pages ↔ todo cycle** ✅ COMPLETED
   - Moved `htmlTodolistSectionContent` and `todoListHtmlContent` from `pages.HtmlElements` to `todo.TodoHtmlRendering`
   - Removed `pages.Styles` class, promoted `BOX_STYLE` to top-level const in `HtmlElements.kt`
   - Removed dead `IndexedFormBuilder.hiddenField` method
   - Verified: `./gradlew cnavDsm` — pages↔todo cycle gone

2. **Break context ↔ root package cycle**
   - `context.SystemContext → ApplicationConfig` and `RoutesKt → context.AppDependencies` create a cycle between `context` and root package
   - Configuration and wiring concerns are entangled
   - Move `ApplicationConfig` into the `context` package, or extract a separate `config` package. Ensure `RoutesKt` only depends on `context` in one direction.
   - `src/main/kotlin/.../ApplicationConfig.kt`, `src/main/kotlin/.../Routes.kt`, `src/main/kotlin/.../context/`
   - Verify: `./gradlew cnavCycles`

3. **Improve Error Handling**
   - Missing comprehensive error handling in configuration loading
   - Application may fail silently or with poor error messages
   - Add proper exception handling in `ApplicationConfig.load()` and other critical paths
   - `src/main/kotlin/no/mikill/kotlin_htmx/Application.kt`

4. **Security Improvements**
   - API keys stored in plain text files, no input validation
   - Security vulnerabilities
   - Implement proper secret management, add input validation for all endpoints
   - Application-wide

## Medium Priority

5. **Split Routes.kt (fan-out: 44)**
   - `RoutesKt` has 44 outgoing calls to 14 distinct classes — highest fan-out in the project
   - Single file knows about all pages/features, changes cascade widely
   - Each feature module should register its own routes. Extract route registration into feature packages (registration, selection, todo, htmx) similar to how Ktor modular routing works.
   - `src/main/kotlin/.../Routes.kt`
   - Verify: `./gradlew cnavComplexity`

6. **Remove dead production code** ✅ COMPLETED
   - Deleted `Styles` class, promoted `BOX_STYLE` to top-level const
   - Removed `IndexedFormBuilder.hiddenField` method (unreferenced)
   - Verified: `./gradlew cnavDead`

7. **Code Organization**
   - Mixed concerns in Application.kt, large functions
   - Reduced maintainability
   - Extract configuration loading, refactor large functions, improve separation of concerns
   - `src/main/kotlin/no/mikill/kotlin_htmx/Application.kt`

8. **Test Coverage Improvements**
   - Limited unit test coverage (8 test files vs 24 source files)
   - Reduced confidence in code changes
   - Add unit tests for domain logic, services, and utilities
   - Create new test files in `src/test/kotlin/`

9. **Reduce SelectMainPage fan-out (28 calls to 12 classes)**
   - Crosses package boundaries — calls into `integration` (LookupClient, LookupResult variants) and `selection` domain
   - Page component knows too much about integration layer
   - Pass pre-resolved data into the page instead of calling LookupClient directly from the page
   - `src/main/kotlin/.../selection/pages/SelectMainPage.kt`

10. **Performance Optimizations**
    - Large number of checkboxes (5000) may impact performance
    - Poor user experience with high loads
    - Implement pagination, lazy loading, or virtualization for checkbox demo
    - HTMX checkbox pages

## Low Priority

11. **Clean up dead methods in BaseSeleniumTest and FakeServerSSESession**
    - `configureDualDriverWindows`, `getScreenDimensions` (BaseSeleniumTest), and `close`, `getCall`, `getCoroutineContext`, `send` (FakeServerSSESession) are unreferenced
    - Dead test infrastructure code; some may be needed for interface compliance (LOW confidence)
    - Review if FakeServerSSESession methods are required by `ServerSentEvent` interface. If so, annotate or document. Remove truly dead methods from BaseSeleniumTest.
    - `src/test/kotlin/.../BaseSeleniumTest.kt`, `src/test/kotlin/.../HtmxCheckboxDemoPageTest.kt`
    - Verify: `./gradlew cnavDead`

12. **Reduce HtmxCheckboxDemoPage fan-out (37 calls to 4 classes)**
    - Third-highest fan-out, heavy self-calling (22 internal calls) and 10 calls to `HtmlRenderUtils`
    - Large page component, candidate for splitting
    - Extract sub-components (checkbox list rendering, SSE event handlers) into separate functions/classes
    - `src/main/kotlin/.../pages/htmx/HtmxCheckboxDemoPage.kt`

13. **Remove hard-coded values**
    - Port 8080, NUMBER_OF_BOXES environment variable hard-coded
    - Configuration scattered
    - Extract to `ApplicationConfig` with proper defaults and validation
    - Application-wide

14. **Documentation**
    - Limited inline documentation and API documentation
    - Reduced developer productivity
    - Add KDoc comments, improve README with setup instructions
    - All source files, README.md

15. **Build Improvements**
    - Could benefit from build optimizations
    - Slower development cycle
    - Consider parallel test execution, build caching improvements
    - `build.gradle.kts`

16. **Logging Improvements**
    - Inconsistent logging patterns
    - Harder to debug issues
    - Standardize logging format, add structured logging
    - Application-wide

17. **Environment Configuration**
    - Environment file handling could be more robust
    - Configuration errors in different environments
    - Add validation for environment variables, better default handling
    - `src/main/kotlin/no/mikill/kotlin_htmx/Application.kt`

18. **Add health check endpoints**
    - No observability for deployment readiness
    - Hard to monitor application health
    - Add `/health` endpoint returning status of key dependencies
    - `src/main/kotlin/.../plugins/` or new `monitoring` package

19. **Add integration tests for critical user journeys**
    - No end-to-end coverage of registration flow, selection flow
    - Regression risk on refactoring
    - Add integration tests covering main user paths (register person, add address, complete registration)
    - `src/test/kotlin/`

## Completed

20. **Dependency Updates** ✅ COMPLETED
    - Latest round: Kotlin 2.3.10→2.3.20, Ktor 3.4.0→3.4.2, ktlint plugin 14.0.1→14.2.0, Selenium 4.40.0→4.43.0. JUnit 6.x skipped (major version bump). Auto-reload enabled with suspend module function and ktor development block.

21. **Dependency Updates (May 2026)** ✅ COMPLETED
    - Kotlin 2.3.20→2.3.21, Ktor 3.4.2→3.4.3, Jackson 2.21.2→2.21.3, ben-manes.versions 0.53.0→0.54.0
    - Rust 1.94.1→1.95.0, htmx.org 2.0.8→2.0.10
    - Skipped: kotlin-logging 5→8 (major), React 18→19 (major pinned), Gradle 9.4.1→9.5.0
