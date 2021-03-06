package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.CachingClient
import uk.gov.justice.digital.hmpps.managerecallsapi.webclient.TimeoutHandlingWebClient

@Component
class PoliceUkApiClient(
  @Autowired internal val policeUkApiWebClient: TimeoutHandlingWebClient,
) : CachingClient<List<Api.PoliceForce>>(policeUkApiWebClient) {
  // As critical Recall data we likely should have more robust access than calling out everytime to a public website,
  // e.g. caching the responses locally, perhaps introducing a boolean active property etc.

  // Also note: no mock implementation: real is used also for all tests - likely responses will be cached in future anyway

  fun getAllPoliceForces(): Mono<List<Api.PoliceForce>> =
    getResponse("/forces", object : ParameterizedTypeReference<List<Api.PoliceForce>>() {})

  fun findById(policeForceId: PoliceForceId): Mono<Api.PoliceForce> =
    getAllPoliceForces().mapNotNull { list ->
      list.firstOrNull { it.id == policeForceId }
    }
}
