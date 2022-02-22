package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("prisonApi")
class PrisonApiHealth(
  webClient: WebClient,
  @Value("prisonApi") componentName: String,
  @Value("\${prisonApi.endpoint.url}") endpointUrl: String
) : PingHealthCheck(webClient, componentName, "$endpointUrl/health/ping")
