package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("prisonerOffenderSearch")
class PrisonerOffenderSearchHealth(
  webClientNoAuthNoMetrics: WebClient,
  @Value("prisonerOffenderSearch") componentName: String,
  @Value("\${prisonerSearch.endpoint.url}") prisonerSearchEndpointUrl: String
) : PingHealthCheck(webClientNoAuthNoMetrics, componentName, "$prisonerSearchEndpointUrl/health/ping")
