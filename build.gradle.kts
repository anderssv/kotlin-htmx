import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.2.3"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.gradleup.shadow") version "8.3.8"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    application
}

group = "no.mikill.kotlin-htmx"
version = "0.0.1"
val mainClassString = "no.mikill.kotlin_htmx.ApplicationKt"
application {
    mainClass.set(mainClassString)
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        manifest {
            attributes["Main-Class"] = mainClassString
        }
        archiveBaseName.set("kotlin-htmx")
        mergeServiceFiles()
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sse:$ktorVersion")
    implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")

    // HTMX support (commented out - modules may not be available in this Ktor version)
    implementation("io.ktor:ktor-htmx:$ktorVersion")
    implementation("io.ktor:ktor-htmx-html:$ktorVersion")
    implementation("io.ktor:ktor-server-htmx:$ktorVersion")

    implementation("org.hibernate.validator:hibernate-validator:9.1.0.Final")
    implementation("org.glassfish.expressly:expressly:6.0.0")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.nfeld.jsonpathkt:jsonpathkt:2.0.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")

    implementation("org.graalvm.sdk:graal-sdk:24.2.2")
    implementation("org.graalvm.js:js:24.2.2")
    implementation("org.graalvm.js:js-scriptengine:24.2.2")

    testImplementation("org.assertj:assertj-core:3.27.6")

    // JUnit 5 dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    // Selenium dependencies
    testImplementation("org.seleniumhq.selenium:selenium-java:4.38.0")
    testImplementation("io.github.bonigarcia:webdrivermanager:6.2.0")
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    version.set("1.7.1")
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.JSON)
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
