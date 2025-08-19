plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.gtlsystems"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
		vendor = JvmVendorSpec.ORACLE
	}
}

// JVM 버전 명시적 설정
tasks.withType<JavaCompile> {
	sourceCompatibility = "17"
	targetCompatibility = "17"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
		jvmTarget = "17"
		freeCompilerArgs += "-Xjsr305=strict"
	}
}

// 디버깅을 위한 JVM 옵션 설정
tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
	jvmArgs = listOf(
		"-Djava.version=17",
		"-Dfile.encoding=UTF-8",
		"-Duser.language=ko",
		"-Duser.country=KR"
	)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	// SpringDoc OpenAPI + Swagger UI (WebFlux용) - Spring Boot 3.4.4와 호환되는 최신 버전
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.6")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	//Sun Track Algorithm
	implementation("net.e175.klaus:solarpositioning:2.0.3")

	//Orekit
	implementation("org.orekit:orekit:13.0.2")

}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
