package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.http.HttpHeader
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@Suppress("SpringJavaConstructorAutowiringInspection")
open class HealthServer(config: WireMockConfiguration, private val healthCheckPath: String) : WireMockServer(config) {

  constructor(port: Int, healthCheckPath: String) :
    this(WireMockConfiguration().apply { port(port) }, healthCheckPath)

  fun isHealthy() = healthCheck(HttpStatus.OK)

  fun isUnhealthy() = healthCheck(HttpStatus.INTERNAL_SERVER_ERROR)

  fun isSlow(status: HttpStatus, delay: Int) = healthCheck(status, delay)

  private fun healthCheck(status: HttpStatus, delay: Int = 0) =
    this.stubFor(
      get(healthCheckPath).willReturn(
        aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withStatus(status.value())
          .withFixedDelay(delay)
      )
    )

  fun stubCallWithException(url: String) {
    this.stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)
        )
    )
  }

  fun delaySearch(url: String, timeoutMillis: Int) {
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withHeaders(
              com.github.tomakehurst.wiremock.http.HttpHeaders(
                HttpHeader(
                  HttpHeaders.CONTENT_TYPE,
                  MediaType.APPLICATION_JSON_VALUE
                )
              )
            )
            .withFixedDelay(timeoutMillis)
            .withStatus(HttpStatus.OK.value())
        )
    )
  }
}
