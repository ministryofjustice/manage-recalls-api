package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest

@Component
class PrisonerOffenderSearchMockServer(
  @Autowired @Qualifier("apiClientJwt") private val apiClientJwt: String,
  @Autowired private val objectMapper: ObjectMapper
) : HealthServer(9092, "/health/ping") {

  fun prisonerSearchRespondsWith(
    request: PrisonerSearchRequest,
    status: HttpStatus
  ) {
    stubFor(
      post(urlEqualTo("/prisoner-search/match-prisoners"))
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(request)))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withStatus(status.value())
        )
    )
  }

  fun prisonerSearchRespondsWith(request: PrisonerSearchRequest, responseBody: List<Prisoner>?) {
    stubFor(
      post(urlEqualTo("/prisoner-search/match-prisoners"))
        .withHeader(AUTHORIZATION, equalTo("Bearer $apiClientJwt"))
        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(request)))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withStatus(OK.value())
            .withBody(objectMapper.writeValueAsString(responseBody))
        )
    )
  }

  fun getPrisonerByNomsNumberRespondsWith(nomsNumber: NomsNumber, responseBody: Prisoner) {
    stubFor(
      get(urlEqualTo("/prisoner/$nomsNumber"))
        .withHeader(AUTHORIZATION, equalTo("Bearer $apiClientJwt"))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withStatus(OK.value())
            .withBody(objectMapper.writeValueAsString(responseBody))
        )
    )
  }

  fun delaySearch(request: PrisonerSearchRequest, timeoutMillis: Int) {
    stubFor(
      post(urlEqualTo("/prisoner-search/match-prisoners"))
        .withRequestBody(equalToJson(objectMapper.writeValueAsString(request)))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withFixedDelay(timeoutMillis)
            .withStatus(OK.value())
        )
    )
  }
}
