package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Movement

@Component
class PrisonApiMockServer(
  @Autowired @Qualifier("apiClientJwt") private val apiClientJwt: String,
  @Autowired private val objectMapper: ObjectMapper
) : HealthServer(9097, "/health/ping") {

  fun latestMovementsRespondsWith(
    request: Set<NomsNumber>,
    responseBody: List<Movement>?,
    withAuthorization: Boolean = true
  ) {
    var post = post(urlEqualTo("/api/movements/offenders/?latestOnly=true&movementTypes=ADM"))
    if (withAuthorization) {
      post = post.withHeader(AUTHORIZATION, equalTo("Bearer $apiClientJwt"))
    }
    post = post.withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
      .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
      .withRequestBody(equalToJson(objectMapper.writeValueAsString(request)))
      .willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)))
          .withStatus(OK.value())
          .withBody(objectMapper.writeValueAsString(responseBody))
      )
    stubFor(post)
  }
}
