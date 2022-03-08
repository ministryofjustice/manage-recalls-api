package uk.gov.justice.digital.hmpps.managerecallsapi.register

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName

@Component
class CourtRegisterClient(
  @Autowired internal val courtRegisterWebClient: WebClient,
  @Value("\${courtRegister.endpoint.url}") val courtRegisterEndpointUrl: String,
  @Value("\${clientApi.timeout}") val timeout: Long,
  @Autowired private val meterRegistry: MeterRegistry,
) : ErrorHandlingClient(courtRegisterWebClient, courtRegisterEndpointUrl, timeout, meterRegistry) {

  fun getAllCourts(): Mono<List<Court>> =
    getResponse("/courts/all", object : ParameterizedTypeReference<List<Court>>() {})

  fun findById(courtId: CourtId): Mono<Court> =
    getResponseWith404Handling("/courts/id/$courtId", object : ParameterizedTypeReference<Court>() {})

  data class Court(
    val courtId: CourtId,
    val courtName: CourtName
  )
}
