plugins {
	id("org.jetbrains.kotlin.jvm") version coreLibs.versions.kotlin
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
	implementation(coreLibs.kotlinxSerializationPlugin)
}
