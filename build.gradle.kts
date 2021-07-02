plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.0"
  kotlin("plugin.spring") version "1.5.10"
  kotlin("plugin.jpa") version "1.5.20"
}

repositories {
  mavenCentral()
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.security:spring-security-oauth2-client")

  implementation("io.springfox:springfox-boot-starter:3.0.0")
  implementation("org.flywaydb:flyway-core:7.10.0")
  implementation("org.postgresql:postgresql:42.2.22")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")

  "5.6.3".let { junitVersion ->
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
  }

  testImplementation("io.mockk:mockk:1.10.0")
  testImplementation("com.natpryce:hamkrest:1.7.0.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.flywaydb.flyway-test-extensions:flyway-spring-test:7.0.0")
  testImplementation("io.zonky.test:embedded-database-spring-test:2.0.1")
  testImplementation("io.zonky.test:embedded-postgres:1.3.0")
  testImplementation("io.projectreactor:reactor-test")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
    }
  }

  test {
    exclude("**/*RealPdfDocumentGeneratorTest*")
  }
}

task<Test>("documentGenerationTest") {
  description = "Runs the documetn generation tests against gotenberg"
  group = "verification"
  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath
  include("**/*RealPdfDocumentGeneratorTest*")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(16))
    vendor.set(JvmVendorSpec.matching("AdoptOpenJDK"))
  }
}
