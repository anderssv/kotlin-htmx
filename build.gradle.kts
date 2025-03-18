import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.1"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.gradleup.shadow") version "8.3.6"
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

    implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    implementation("org.glassfish.expressly:expressly:5.0.0")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("org.jetbrains:kotlin-css-jvm:1.0.0-pre.156-kotlin-1.5.0")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.nfeld.jsonpathkt:jsonpathkt:2.0.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")

    // Selenium dependencies
    testImplementation("org.seleniumhq.selenium:selenium-java:4.29.0")
    testImplementation("io.github.bonigarcia:webdrivermanager:5.9.3")
}

tasks.withType<DependencyUpdatesTask> {
    resolutionStrategy {
        componentSelection {
            all {
                if (candidate.version.contains("beta", true)
                    || candidate.version.contains("-rc", true)
                    || candidate.version.endsWith("-M1")
                    || candidate.version.endsWith(".CR1")) {
                    reject("Not a release")
                }
            }
        }
    }
}
