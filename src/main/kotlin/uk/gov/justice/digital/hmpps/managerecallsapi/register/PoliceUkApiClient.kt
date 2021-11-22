package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId

@Component
class PoliceUkApiClient {
  // As critical Recall data we likely should have more robust access than calling out everytime to a public website,
  // e.g. caching the responses locally, perhaps introducing a boolean active property etc.

  // Also note: no mock implementation: real is used also for all tests - likely responses will be cached in future anyway

  @Autowired
  internal lateinit var policeUkApiWebClient: WebClient

  fun getAllPoliceForces(): Mono<List<Api.PoliceForce>> {
    return policeUkApiWebClient
      .get()
      .uri("/forces")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Api.PoliceForce>>() {})
  }

  fun findById(policeForceId: PoliceForceId): Mono<Api.PoliceForce> {
    return policeUkApiWebClient
      .get()
      .uri("/forces/$policeForceId")
      .retrieve()
      .bodyToMono(Api.PoliceForce::class.java)
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        when (exception.rawStatusCode) {
          404 -> Mono.empty()
          else -> Mono.error(exception)
        }
      }
  }
}
