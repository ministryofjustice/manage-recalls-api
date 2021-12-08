package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName

@Component
class PrisonRegisterClient(
  @Autowired private val prisonRegisterWebClient: WebClient,
  @Value("\${clientApi.timeout}") val timeout: Long
) : ErrorHandlingClient(prisonRegisterWebClient, timeout) {

  fun getAllPrisons(): Mono<List<Prison>> =
    getResponse("/prisons", object : ParameterizedTypeReference<List<Prison>>() {})

  fun findPrisonById(prisonId: PrisonId): Mono<Prison> =
    getResponseWith404Handling("/prisons/id/$prisonId", object : ParameterizedTypeReference<Prison>() {})
}

data class Prison(val prisonId: PrisonId, val prisonName: PrisonName, val active: Boolean)
