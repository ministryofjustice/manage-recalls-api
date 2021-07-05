package uk.gov.justice.digital.hmpps.managerecallsapi.search

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import java.time.LocalDate

@Component
class PrisonerOffenderSearchClient {

  @Autowired
  @Qualifier("prisonerOffenderSearchWebClient")
  internal lateinit var webClient: AuthenticatingRestClient

  fun prisonerSearch(searchRequest: SearchRequest): Mono<List<Prisoner>> =
    webClient
      .post("/prisoner-search/match-prisoners", PrisonerSearchRequest(searchRequest.nomsNumber))
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<Prisoner>>() {})
}

data class Prisoner(
  val prisonerNumber: String? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val firstName: String? = null,
  val middleNames: String? = "",
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val status: String? = null,
  val bookNumber: String? = null
)

data class PrisonerSearchRequest(val prisonerIdentifier: String)
