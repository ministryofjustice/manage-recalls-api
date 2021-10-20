package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String

@Component
class HmppsAuthMockServer(
  @Autowired @Qualifier("apiClientJwt") private val apiClientJwt: String,
  @Value("\${spring.security.oauth2.client.registration.offender-search-client.client-id}") private val apiClientId: String,
  @Value("\${spring.security.oauth2.client.registration.offender-search-client.client-secret}") private val apiClientSecret: String
) : HealthServer(9090, "/auth/health/ping") {

  private val oauthClientToken = "$apiClientId:$apiClientSecret".toByteArray().encodeToBase64String()

  fun stubClientToken() {
    stubFor(
      post(urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withHeaders(
              HttpHeaders(
                HttpHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE),
                HttpHeader(AUTHORIZATION, "Basic $oauthClientToken")
              )
            )
            .withBody(
              """{
                  "token_type": "bearer",
                  "access_token": "$apiClientJwt"
              }
              """.trimIndent()
            )
        )
    )
  }
}
