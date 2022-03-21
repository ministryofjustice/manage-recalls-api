package uk.gov.justice.digital.hmpps.managerecallsapi.register

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.register.CourtRegisterClient.Court

@Component
class CourtRegisterClient(
  @Autowired internal val courtRegisterWebClient: TimeoutHandlingWebClient,
) : CachingClient<List<Court>>(courtRegisterWebClient) {

  fun getAllCourts(): Mono<List<Court>> =
    getResponse("/courts/all", object : ParameterizedTypeReference<List<Court>>() {})

  fun findById(courtId: CourtId): Mono<Court> =
    getAllCourts().mapNotNull { list ->
      list.firstOrNull { it.courtId == courtId }
    }

  data class Court(
    val courtId: CourtId,
    val courtName: CourtName
  )
}
