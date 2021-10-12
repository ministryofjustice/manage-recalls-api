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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.court.CourtRegisterClient.Court

@Component
class CourtRegisterMockServer(
  @Autowired private val objectMapper: ObjectMapper
) : WireMockServer(9095) {

  fun stubCourts() {
    val courts =
      listOf(
        Court(CourtId("ACCRYC"), CourtName("Accrington Youth Court")),
        Court(CourtId("BANBCT"), CourtName("Banbury County Court")),
        Court(CourtId("CARLCT"), CourtName("Carlisle Combined Court Centre")),
        Court(CourtId("HVRFCT"), CourtName("Haverfordwest County Court")),
      )
    stubAll(courts)
    courts.forEach { stub(it) }
  }

  fun stubAll(courts: List<Court>) {
    stubGet("/courts/all", courts)
  }

  fun stub(court: Court) {
    stubGet("/courts/id/${court.courtId}", court)
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
