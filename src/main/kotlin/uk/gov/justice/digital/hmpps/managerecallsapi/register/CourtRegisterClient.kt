package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName

@Component
class CourtRegisterClient(
  @Autowired
  internal val courtRegisterWebClient: WebClient
) : ErrorHandlingClient(courtRegisterWebClient) {

  fun getAllCourts(): Mono<List<Court>> =
    getResponse("/courts/all", object : ParameterizedTypeReference<List<Court>>() {})

  fun findById(courtId: CourtId): Mono<Court> =
    getResponseWith404Handling("/courts/id/$courtId", object : ParameterizedTypeReference<Court>() {})

  data class Court(
    val courtId: CourtId,
    val courtName: CourtName
  )
}
