package uk.gov.justice.digital.hmpps.managerecallsapi.register.court

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
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

  data class Court(
    val courtId: CourtId? = null,
    val courtName: CourtName? = null
  )
}
