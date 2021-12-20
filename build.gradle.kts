plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.16"
  kotlin("plugin.spring") version "1.5.10"
  kotlin("plugin.jpa") version "1.5.20"
  kotlin("plugin.serialization") version "1.4.31"
  id("org.owasp.dependencycheck") version "6.5.0.1"
}

repositories {
  mavenCentral()
}

configurations {
  testImplementation {
    exclude(group = "org.junit.vintage")
    exclude(group = "junit")
    exclude(group = "com.vaadin.external.google", module = "android-json")
  }
}

dependencyCheck {
  // Fail the build when there is a finding with "medium" severity.
  // failBuildOnCVSS = 4
  // Only check the compile classpath and ignore findings in other configurations.
  // scanConfigurations = ['debugCompileClasspath', 'acceptanceCompileClasspath', 'productionCompileClasspath']
  suppressionFiles.add("$rootDir/owasp.suppression.xml")
}

// Force log4j2.version to 2.17 for CVE-2021-45105
project.extensions.extraProperties["log4j2.version"] = "2.17.0"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator:2.5.5")
  implementation("io.micrometer:micrometer-registry-prometheus:1.7.5")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.security:spring-security-oauth2-client")

  implementation("io.springfox:springfox-boot-starter:3.0.0")
  implementation("org.flywaydb:flyway-core:7.14.1")
  implementation("org.postgresql:postgresql:42.2.22")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

  implementation("software.amazon.awssdk:s3:2.17.46")
  implementation("org.http4k:http4k-format-jackson:4.13.1.0")

  implementation("io.sentry:sentry-spring-boot-starter:5.2.1")
  implementation("io.sentry:sentry-logback:5.2.1")
  implementation("com.github.librepdf:openpdf:1.3.26")
  implementation("com.github.librepdf:openpdf-fonts-extra:1.3.26")
  implementation("xyz.capybara:clamav-client:2.0.2")
  implementation("dev.forkhandles:result4k:1.11.2.1")
  implementation("net.sf.jmimemagic:jmimemagic:0.1.5") {
    exclude("log4j", "log4j")
  }
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")

  "5.7.0".let { junitVersion ->
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
  }

  testImplementation("io.mockk:mockk:1.12.0")
  testImplementation("com.natpryce:hamkrest:1.8.0.1")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.flywaydb.flyway-test-extensions:flyway-spring-test:7.0.0")
  testImplementation("io.zonky.test:embedded-database-spring-test:2.1.0")
  testImplementation("io.zonky.test:embedded-postgres:1.3.1")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("com.ninja-squad:springmockk:3.0.1")

  testImplementation("au.com.dius.pact.provider:junit5:4.2.14")
  testImplementation("au.com.dius.pact.provider:junit5spring:4.2.14")
  testImplementation("org.http4k:http4k-testing-approval:4.13.1.0")
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_16.toString()
    }
  }

  test {
    useJUnitPlatform {
      exclude("**/*GotenbergIntegrationTest*")
      exclude("**/*GotenbergComponentTest*")
      exclude("**/*PactTest*")
    }
  }

  register<Test>("verifyPactAndPublish") {
    description = "Run and publish Pact provider tests"
    group = "verification"

    systemProperty("pact.provider.tag", System.getenv("PACT_PROVIDER_TAG"))
    systemProperty("pact.provider.version", System.getenv("PACT_PROVIDER_VERSION"))
    systemProperty("pact.verifier.publishResults", System.getenv("PACT_PUBLISH_RESULTS") ?: "false")
    systemProperty("pactbroker.host", System.getenv("PACTBROKER_HOST") ?: "pact-broker-prod.apps.live-1.cloud-platform.service.justice.gov.uk")

    useJUnitPlatform {
      include("**/*PactTest*")
    }
  }
}

task<Test>("documentGenerationTest") {
  description = "Runs the document generation tests against gotenberg"
  group = "verification"
  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath
  include("**/*GotenbergIntegrationTest*")
  include("**/*GotenbergComponentTest*")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(16))
    vendor.set(JvmVendorSpec.matching("AdoptOpenJDK"))
  }
}
