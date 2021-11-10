package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
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
      WireMock.get(healthCheckPath).willReturn(
        WireMock.aResponse()
          .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .withStatus(status.value())
          .withFixedDelay(delay)
      )
    )
}
