package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("prisonRegister")
class PrisonRegisterHealth(
  webClient: WebClient,
  @Value("\${prisonRegister.endpoint.url}") prisonRegisterEndpointUrl: String
) : PingHealthCheck(webClient, "$prisonRegisterEndpointUrl/health")
