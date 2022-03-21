package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
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

@Component
class PrisonerOffenderSearchMockServer(
  @Autowired @Qualifier("apiClientJwt") private val apiClientJwt: String,
  @Autowired private val objectMapper: ObjectMapper
) : HealthServer(9092, "/health/ping") {

  fun getPrisonerByNomsNumberReturnsWith(nomsNumber: NomsNumber, status: HttpStatus) {
    stubFor(
      get(urlEqualTo("/prisoner/$nomsNumber"))
        .withHeader(AUTHORIZATION, equalTo("Bearer $apiClientJwt"))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withStatus(status.value())
        )
    )
  }

  fun getPrisonerByNomsNumberRespondsWith(
    nomsNumber: NomsNumber,
    responseBody: Prisoner,
    withAuthorization: Boolean = true
  ) {
    var get = get(urlEqualTo("/prisoner/$nomsNumber"))
    if (withAuthorization)
      get = get.withHeader(AUTHORIZATION, equalTo("Bearer $apiClientJwt"))
    get = get
      .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
      .willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
          .withStatus(OK.value())
          .withBody(objectMapper.writeValueAsString(responseBody))
      )

    stubFor(get)
  }

  fun delayGetPrisoner(nomsNumber: NomsNumber, timeoutMillis: Int) {
    stubFor(
      get(urlEqualTo("/prisoner/$nomsNumber"))
        .withHeader(AUTHORIZATION, equalTo("Bearer $apiClientJwt"))
        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
            .withFixedDelay(timeoutMillis)
            .withStatus(OK.value())
        )
    )
  }
}
