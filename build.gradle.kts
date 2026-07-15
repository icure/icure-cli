import com.github.jk1.license.render.CsvReportRenderer
import com.github.jk1.license.render.ReportRenderer

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
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
// Overridable so release builds can stamp the distribution with the tag version:
// ./gradlew :icure-cli:distZip -PcliVersion=1.3.0
version = providers.gradleProperty("cliVersion").getOrElse("1.2-SNAPSHOT")

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

dependencies {
    implementation(libs.bundles.io.ktor)
    implementation(libs.com.github.ajalt.clikt)
    implementation(libs.com.google.cloud.storage)
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.serialization.json)
    implementation(libs.icure.sdk)
    implementation(libs.ajalt.clikt)
    implementation(libs.jline)
    implementation(libs.logback)
    implementation(libs.xerces)
    implementation(libs.io.github.reidsync.kotlin.json.patch)
    implementation(libs.apache.commons)
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
