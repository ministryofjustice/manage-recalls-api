package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("gotenberg")
class GotenbergHealth(
  webClientNoAuthNoMetrics: WebClient,
  @Value("gotenberg") componentName: String,
  @Value("\${gotenberg.endpoint.url}") gotenbergEndpointUrl: String
) : PingHealthCheck(webClientNoAuthNoMetrics, componentName, "$gotenbergEndpointUrl/health")
