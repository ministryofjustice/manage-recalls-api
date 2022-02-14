package uk.gov.justice.digital.hmpps.managerecallsapi.integration

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import io.swagger.v3.parser.OpenAPIV3Parser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIntegrationTest(@Autowired private val webTestClient: WebTestClient) {
  @LocalServerPort
  private val randomServerPort = 0

  private val openApiV3path = "/v3/api-docs"

  @Test
  fun `swagger html is available at default location`() {
    webTestClient.get()
      .uri("/swagger-ui/index.html")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `the v3 open api json is valid`() {
    webTestClient.get()
      .uri(openApiV3path)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("messages").doesNotExist()
  }

  @Test
  fun `the v3 open api json is available and contains documentation`() {
    val result = OpenAPIV3Parser().readLocation("http://localhost:$randomServerPort$openApiV3path", null, null)
    assertThat(result.messages.size, equalTo(0))
    assertThat(result.openAPI.paths.size, greaterThan(0))
  }

  @Test
  fun `the v3 open api json contains the date as version number`() {
    webTestClient.get()
      .uri(openApiV3path)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("info.version").isEqualTo(DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
  }
}
