import com.github.jk1.license.render.CsvReportRenderer
import com.github.jk1.license.render.ReportRenderer

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.github.jk1.dependency-license-report") version "2.0"
    application
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(CsvReportRenderer())
}

buildscript {
    dependencies {
        classpath("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    }
}

group = "com.icure"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

val ktorVersion = "2.3.9"

dependencies {
    implementation(libs.bundles.io.ktor)
    implementation(libs.com.github.ajalt.clikt)
    implementation(libs.org.jetbrains.kotlinx.serialization.json)
    implementation(libs.icure.sdk)
    implementation(libs.ajalt.clikt)
    implementation(libs.jline)
    implementation(libs.logback)
    implementation(libs.xerces)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf("Main-Class" to "com.icure.cli.MainKt"))
    }
}

application {
    mainClass.set("com.icure.cli.MainKt")
}

distributions {
    main {
        contents {
            from("./plugins") {
                into("plugins")
            }
            from("./USAGE.md")
        }
    }
}
