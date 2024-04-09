val kotlin = "1.9.20"
val iCureE2eTestVersion = "0.0.1-geb49419b5a"
val kotlinx = "1.7.3"

plugins {
	id("org.jetbrains.kotlin.jvm") version "1.9.20"
}

repositories {
	mavenCentral()
	maven { url = uri("https://maven.taktik.be/content/groups/public") }
	maven { url = uri("https://jitpack.io") }
}

version = "0.0.1-SNAPSHOT"

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		languageVersion = "1.9"
		jvmTarget = "21"
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
	implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
	implementation("com.google.guava:guava:31.0.1-jre")
	implementation("com.github.kotlinx.ast:grammar-kotlin-parser-antlr-kotlin:0.1.0")
	implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = "2.13.5")
	implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.13.5")

	implementation(group = "io.icure", name = "icure-e2e-test-setup", version = iCureE2eTestVersion)
	implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = kotlinx)
}
