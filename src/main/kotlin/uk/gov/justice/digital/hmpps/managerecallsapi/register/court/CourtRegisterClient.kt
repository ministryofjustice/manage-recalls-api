package uk.gov.justice.digital.hmpps.managerecallsapi.register.court

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName

@Component
class CourtRegisterClient {

  @Autowired
  internal lateinit var courtRegisterWebClient: WebClient

  fun getAllCourts(): Mono<List<Court>> {
    return courtRegisterWebClient
      .get()
      .uri("/courts/all")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Court>>() {})
  }

  fun findById(courtId: CourtId): Mono<Court> {
    return courtRegisterWebClient
      .get()
      .uri("/courts/id/$courtId")
      .retrieve()
      .bodyToMono(Court::class.java)
      .onErrorResume(WebClientResponseException::class.java) { exception ->
        when (exception.rawStatusCode) {
          404 -> Mono.empty()
          else -> Mono.error(exception)
        }
      }
  }

  data class Court(
    val courtId: CourtId,
    val courtName: CourtName
  )
}
