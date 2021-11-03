package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("courtRegister")
class CourtRegisterHealth(
  webClient: WebClient,
  @Value("courtRegister") componentName: String,
  @Value("\${courtRegister.endpoint.url}") prisonRegisterEndpointUrl: String
) : PingHealthCheck(webClient, componentName, "$prisonRegisterEndpointUrl/health/ping")
