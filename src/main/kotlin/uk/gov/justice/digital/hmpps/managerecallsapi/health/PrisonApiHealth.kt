package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("prisonApi")
class PrisonApiHealth(
  webClientNoAuthNoMetrics: WebClient,
  @Value("prisonApi") componentName: String,
  @Value("\${prisonApi.endpoint.url}") endpointUrl: String
) : PingHealthCheck(webClientNoAuthNoMetrics, componentName, "$endpointUrl/health/ping")
