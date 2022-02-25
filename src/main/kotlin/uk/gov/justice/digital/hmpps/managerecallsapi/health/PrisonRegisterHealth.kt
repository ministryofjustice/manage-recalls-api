package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("prisonRegister")
class PrisonRegisterHealth(
  webClientNoAuthNoMetrics: WebClient,
  @Value("prisonRegister") componentName: String,
  @Value("\${prisonRegister.endpoint.url}") prisonRegisterEndpointUrl: String
) : PingHealthCheck(webClientNoAuthNoMetrics, componentName, "$prisonRegisterEndpointUrl/health/ping")
