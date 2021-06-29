package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class GotenbergHealth(
  webClient: WebClient,
  @Value("\${gotenberg.endpoint.url}") gotenbergEndpointUrl: String
) : PingHealthCheck(webClient, "$gotenbergEndpointUrl/ping")