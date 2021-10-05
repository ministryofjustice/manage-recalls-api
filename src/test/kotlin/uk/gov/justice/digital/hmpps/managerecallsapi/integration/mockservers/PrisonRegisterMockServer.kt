package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient.Prison

@Component
class PrisonRegisterMockServer(
  @Autowired private val objectMapper: ObjectMapper
) : WireMockServer(9094) {

  fun respondsWith200() {
    stubFor(
      get(urlEqualTo("/prisons"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withBody(
              objectMapper.writeValueAsString(
                listOf(
                  Prison(PrisonId("MWI"), PrisonName("Medway (STC)"), true),
                  Prison(PrisonId("AKI"), PrisonName("Acklington (HMP)"), false),
                  Prison(PrisonId("BMI"), PrisonName("Birmingham (HMP)"), true),
                  Prison(PrisonId("KTI"), PrisonName("KTI (HMP)"), true),
                  Prison(PrisonId("BAI"), PrisonName("BAI (HMP)"), true),
                  Prison(PrisonId("BLI"), PrisonName("BLI (HMP)"), true),
                )
              )
            )
            .withStatus(OK.value())
        )
    )
  }

  fun isHealthy() {
    healthCheck(OK)
  }

  fun isUnhealthy() {
    healthCheck(HttpStatus.INTERNAL_SERVER_ERROR)
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
