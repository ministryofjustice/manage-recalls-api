package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component

@Component
class PrisonRegisterMockServer(
  @Autowired @Qualifier("apiClientJwt") private val apiClientJwt: String,
  @Autowired private val objectMapper: ObjectMapper
) : WireMockServer(9094) {

  fun prisonerSearchRespondsWith200() {
    stubFor(
      get(WireMock.urlEqualTo("/prisons"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withBody(
              "[{\"prisonId\":\"MWI\",\"prisonName\":\"Medway (STC)\",\"active\":true}," +
                "{\"prisonId\":\"AKI\",\"prisonName\":\"Acklington (HMP)\",\"active\":false}," +
                "{\"prisonId\":\"BMI\",\"prisonName\":\"Birmingham (HMP)\",\"active\":true}]"
            )
            .withStatus(OK.value())
        )
    )
  }

  private fun healthCheck(status: HttpStatus) =
    this.stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
          .withStatus(status.value())
      )
    )
}
