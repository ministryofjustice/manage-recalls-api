package uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.stubbing.StubMapping
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

  fun stubGetWithException(url: String) {
    stubCallWithException(get(urlEqualTo(url)))
  }

  fun stubPostWithException(url: String) {
    stubCallWithException(post(urlEqualTo(url)))
  }

  private fun stubCallWithException(mappingBuilder: MappingBuilder) {
    this.stubFor(
      mappingBuilder
        .willReturn(
          aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)
        )
    )
  }

  fun delayGet(url: String, timeoutMillis: Int): StubMapping =
    delayCall(get(urlEqualTo(url)), timeoutMillis)

  fun delayPost(url: String, timeoutMillis: Int): StubMapping =
    delayCall(post(urlEqualTo(url)), timeoutMillis)

  private fun delayCall(
    mappingBuilder: MappingBuilder,
    timeoutMillis: Int
  ) = stubFor(
    mappingBuilder
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

  protected fun <T> stubGet(url: String, response: List<T>, objectMapper: ObjectMapper) {
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
            .withBody(
              objectMapper.writeValueAsString(response)
            )
            .withStatus(HttpStatus.OK.value())
        )
    )
  }
}
