package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.prison.Prison

@Component
class PrisonRegisterMockServer(
  @Autowired private val objectMapper: ObjectMapper
) : WireMockServer(
  WireMockConfiguration().apply {
    port(9094)
    extensions(
      ResponseTemplateTransformer.builder()
        .global(false)
        .build()
    )
  }
) {

  fun stubPrisons() {
    val prisons = listOf(
      Prison(PrisonId("MWI"), PrisonName("Medway (STC)"), true),
      Prison(PrisonId("AKI"), PrisonName("Acklington (HMP)"), false),
      Prison(PrisonId("BMI"), PrisonName("Birmingham (HMP)"), true),
      Prison(PrisonId("KTI"), PrisonName("KTI (HMP)"), true),
      Prison(PrisonId("BAI"), PrisonName("BAI (HMP)"), true),
      Prison(PrisonId("BLI"), PrisonName("BLI (HMP)"), true)
    )
    stubAllPrisons(prisons)
    prisons.forEach { stubPrison(it) }
  }

  fun stubAllPrisons(prisons: List<Prison>) {
    stubGet("/prisons", prisons)
  }

  fun stubPrison(prison: Prison) {
    stubGet("/prisons/id/${prison.prisonId}", prison)
  }

  fun stubFindAnyPrisonById() {
    stubFor(
      get(urlPathMatching("/prisons/id/(.*)"))
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withBody(
              "{\n" +
                "      \"prisonId\": \"{{request.path.[2]}}\",\n" +
                "      \"prisonName\": \"Test prison {{request.path.[2]}}\",\n" +
                "      \"active\": true\n" +
                "    }"
            )
            .withTransformers("response-template")
        )
    )
  }

  fun <T> stubGet(url: String, response: T) {
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withBody(
              objectMapper.writeValueAsString(response)
            )
            .withStatus(OK.value())
        )
    )
  }

  fun isHealthy() {
    healthCheck(OK)
  }

  fun isUnhealthy() {
    healthCheck(INTERNAL_SERVER_ERROR)
  }

  private fun healthCheck(status: HttpStatus) =
    this.stubFor(
      get("/health").willReturn(
        aResponse()
          .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
          .withStatus(status.value())
      )
    )
}
