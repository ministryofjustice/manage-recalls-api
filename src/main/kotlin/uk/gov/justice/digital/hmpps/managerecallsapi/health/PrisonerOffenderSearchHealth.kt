package uk.gov.justice.digital.hmpps.managerecallsapi.health

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class PrisonerOffenderSearchHealth(
  webClient: WebClient,
  @Value("\${prisonerSearch.endpoint.url}") prisonerSearchEndpointUrl: String
) : PingHealthCheck(webClient, prisonerSearchEndpointUrl)
