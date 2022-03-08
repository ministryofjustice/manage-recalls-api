package uk.gov.justice.digital.hmpps.managerecallsapi.register

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId

@Component
class PrisonRegisterClient(
  @Autowired private val prisonRegisterWebClient: WebClient,
  @Value("\${prisonRegister.endpoint.url}") val prisonRegisterEndpointUrl: String,
  @Value("\${clientApi.timeout}") val timeout: Long,
  @Autowired private val meterRegistry: MeterRegistry,
) : ErrorHandlingClient(prisonRegisterWebClient, prisonRegisterEndpointUrl, timeout, meterRegistry) {

  fun getAllPrisons(): Mono<List<Api.Prison>> =
    getResponse("/prisons", object : ParameterizedTypeReference<List<Api.Prison>>() {})

  fun findPrisonById(prisonId: PrisonId): Mono<Api.Prison> =
    getResponseWith404Handling("/prisons/id/$prisonId", object : ParameterizedTypeReference<Api.Prison>() {})
}
