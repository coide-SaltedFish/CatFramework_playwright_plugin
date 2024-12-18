import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "4.0.3"

    kotlin("jvm") version "1.9.21"
}

group = "org.sereinfish.catcat.framework.playwright"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

val kotlinxHtmlVersion = "0.11.0"
val catFrameVersion = "0.0.206"

dependencies {

    // include for JVM target
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")
    // include for Common module
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")

    implementation("org.sereinfish.catcat.frame:CatFrame:$catFrameVersion")
    implementation("com.microsoft.playwright:playwright:1.43.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.slf4j:slf4j-api:2.0.12")

    testImplementation(kotlin("test"))
}

tasks.shadowJar {
    exclude("org.sereinfish.catcat.frame:CatFrame")

    manifest {
        attributes["CatPluginId"] = "org.sereinfish.catcat.framework.playwright"
    }

    archiveBaseName.set("CatFramework_playwright_plugin")
    archiveVersion.set("0.0.1")
    archiveClassifier.set("org.sereinfish.catcat.framework.playwright.PluginMain")
}

tasks.jar {
    manifest {
        attributes["CatPluginId"] = "org.sereinfish.catcat.framework.playwright"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}