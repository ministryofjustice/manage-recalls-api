package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.github.tomakehurst.wiremock.WireMockServer
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
import java.util.Base64

@Component
class HmppsAuthMockServer(
  @Autowired @Qualifier("apiClientJwt") private val apiClientJwt: String,
  @Value("\${spring.security.oauth2.client.registration.offender-search-client.client-id}") private val apiClientId: String,
  @Value("\${spring.security.oauth2.client.registration.offender-search-client.client-secret}") private val apiClientSecret: String
) : WireMockServer(9090) {

  private val oauthClientToken = Base64.getEncoder().encodeToString("$apiClientId:$apiClientSecret".toByteArray())

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
