package uk.gov.justice.digital.hmpps.managerecallsapi.register

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId

@Component
class PoliceUkApiClient(
  @Autowired internal val policeUkApiWebClient: WebClient,
  @Value("\${policeUkApi.endpoint.url}") val policeUkApiEndpointUrl: String,
  @Value("\${clientApi.timeout}") val timeout: Long,
  @Autowired private val meterRegistry: MeterRegistry,
) : ErrorHandlingClient(policeUkApiWebClient, policeUkApiEndpointUrl, timeout, meterRegistry) {
  // As critical Recall data we likely should have more robust access than calling out everytime to a public website,
  // e.g. caching the responses locally, perhaps introducing a boolean active property etc.

  // Also note: no mock implementation: real is used also for all tests - likely responses will be cached in future anyway

  fun getAllPoliceForces(): Mono<List<Api.PoliceForce>> =
    getResponse("/forces", object : ParameterizedTypeReference<List<Api.PoliceForce>>() {})

  fun findById(policeForceId: PoliceForceId): Mono<Api.PoliceForce> =
    getResponseWith404Handling("/forces/$policeForceId", object : ParameterizedTypeReference<Api.PoliceForce>() {})
}
