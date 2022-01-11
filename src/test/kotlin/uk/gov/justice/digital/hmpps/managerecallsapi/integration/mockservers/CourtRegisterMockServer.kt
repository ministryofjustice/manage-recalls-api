package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient.Court

@Component
class CourtRegisterMockServer(
  @Autowired private val objectMapper: ObjectMapper
) : HealthServer(
  WireMockConfiguration().apply {
    port(9095)
    extensions(ResponseTemplateTransformer.builder().global(false).build())
  },
  "/health/ping"
) {

  fun stubCourts() {
    val courts =
      listOf(
        Court(CourtId("ACCRYC"), CourtName("Accrington Youth Court")),
        Court(CourtId("BANBCT"), CourtName("Banbury County Court")),
        Court(CourtId("CARLCT"), CourtName("Carlisle Combined Court Centre")),
        Court(CourtId("HVRFCT"), CourtName("Haverfordwest County Court")),
        Court(CourtId("SOUTCT"), CourtName("Southport County Court")),
      )
    stubGet("/courts/all", courts)
    courts.forEach { stub(it) }
  }

  fun stub(court: Court) {
    stubGet("/courts/id/${court.courtId}", court)
  }

  private fun <T> stubGet(url: String, response: T) {
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

  fun stubFindAnyCourtById() {
    stubFor(
      get(WireMock.urlPathMatching("/courts/id/(.*)"))
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withBody(
              "{\n" +
                "      \"courtId\": \"{{request.path.[2]}}\",\n" +
                "      \"courtName\": \"Test court {{request.path.[2]}}\"\n" +
                "    }"
            )
            .withTransformers("response-template")
        )
    )
  }
}
