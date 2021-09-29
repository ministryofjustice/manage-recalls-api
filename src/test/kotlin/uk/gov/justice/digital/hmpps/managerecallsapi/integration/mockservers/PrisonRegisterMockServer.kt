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
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.prisonData.PrisonRegisterClient.Prison

@Component
class PrisonRegisterMockServer(
  @Autowired private val objectMapper: ObjectMapper
) : WireMockServer(9094) {

  fun prisonerSearchRespondsWith200() {
    stubFor(
      get(urlEqualTo("/prisons"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withBody(
              objectMapper.writeValueAsString(
                listOf(
                  Prison("MWI", "Medway (STC)", true),
                  Prison("AKI", "Acklington (HMP)", false),
                  Prison("BMI", "Birmingham (HMP)", true),
                )
              )
            )
            .withStatus(OK.value())
        )
    )
  }
}
