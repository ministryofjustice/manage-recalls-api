plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.0"
  kotlin("plugin.spring") version "1.5.10"
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

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")

  "5.6.2".let { junitVersion ->
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
  }

  testImplementation("io.mockk:mockk:1.10.0")
  testImplementation("com.natpryce:hamkrest:1.7.0.3")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.mock-server:mockserver-spring-test-listener:5.11.2")
  testImplementation("org.mock-server:mockserver-junit-jupiter:5.11.2")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(16))
    vendor.set(JvmVendorSpec.matching("AdoptOpenJDK"))
  }
}
