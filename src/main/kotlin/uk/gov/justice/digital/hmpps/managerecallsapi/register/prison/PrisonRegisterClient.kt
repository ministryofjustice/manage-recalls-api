package uk.gov.justice.digital.hmpps.managerecallsapi.register.prison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName

@Component
class PrisonRegisterClient(
  @Autowired private val prisonRegisterWebClient: WebClient
) {

  fun getAllPrisons(): Mono<List<Prison>> {
    return prisonRegisterWebClient
      .get()
      .uri("/prisons")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Prison>>() {})
  }

  fun findPrisonById(prisonId: PrisonId): Mono<Prison> =
    prisonRegisterWebClient
      .get()
      .uri("/prisons/id/$prisonId")
      .retrieve()
      .bodyToMono(Prison::class.java)
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        when (exception.rawStatusCode) {
          404 -> Mono.empty()
          else -> Mono.error(exception)
        }
      }
}

data class Prison(val prisonId: PrisonId, val prisonName: PrisonName, val active: Boolean)
