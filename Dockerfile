FROM eclipse-temurin:21 AS build

WORKDIR /tmp/build

# Copy Gradle wrapper and build files first for dependency caching
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# Download dependencies (this layer is cached if build files don't change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:21
WORKDIR /app

# Install LightningCSS binary (native Rust CSS processor)
# Downloaded from npm registry - contains pre-built binary, no Node.js needed
ARG LIGHTNINGCSS_VERSION=1.30.2
RUN curl -fsSL "https://registry.npmjs.org/lightningcss-cli-linux-x64-gnu/-/lightningcss-cli-linux-x64-gnu-${LIGHTNINGCSS_VERSION}.tgz" \
    | tar -xz -C /usr/local/bin --strip-components=1 package/lightningcss \
    && chmod +x /usr/local/bin/lightningcss

COPY --from=build /tmp/build/build/libs/kotlin-htmx-all.jar .

EXPOSE 8080
ENV TZ="Europe/Oslo"
ENV MALLOC_ARENA_MAX=1
CMD java $JAVA_OPTS -jar kotlin-htmx-all.jar
