package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("hmppsAuth")
class HmppsAuthHealth(
  webClient: WebClient,
  @Value("\${oauth.endpoint.url}") hmppsAuthEndpointUrl: String
) : PingHealthCheck(webClient, "$hmppsAuthEndpointUrl/health/ping")
