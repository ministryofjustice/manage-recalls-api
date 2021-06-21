package uk.gov.justice.digital.hmpps.managerecallsapi.search

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PrisonerOffenderSearchClient {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Autowired
  @Qualifier("prisonerOffenderSearchWebClient")
  internal lateinit var webClient: AuthenticatingRestClient

  fun prisonerSearch(searchRequest: SearchRequest): List<SearchResult> {
    log.info("Sending /prisoner-search/match-prisoners request: $searchRequest")

    return webClient
      .post("/prisoner-search/match-prisoners", PrisonerSearchRequest(searchRequest.nomsNumber))
      .retrieve()
      .bodyToMono(List::class.java)
      .block()!!
      .toSearchResults()
  }

  fun prisonerMatch(searchRequest: SearchRequest): List<SearchResult> {
    log.info("Sending /match-prisoners request: $searchRequest")

    return webClient
      .post("/match-prisoners", PrisonerMatchRequest(searchRequest.nomsNumber))
      .retrieve()
      .bodyToMono(PrisonerMatches::class.java)
      .block()!!
      .toSearchResults()
  }

}

fun List<*>?.toSearchResults() =
  this?.let {
    this.map {
      with(it.prisoner) {
        SearchResult(firstName, lastName, prisonerNumber, dateOfBirth)
      }
    }
  }.orEmpty()

fun PrisonerMatches?.toSearchResults() =
  this?.let {
    matches.map {
      with(it.prisoner) {
        SearchResult(firstName, lastName, prisonerNumber, dateOfBirth)
      }
    }
  }.orEmpty()

data class PrisonerMatches(
  val matches: List<PrisonerMatch> = listOf(),
  val matchedBy: MatchedBy = MatchedBy.NOTHING
)

data class PrisonerMatch(
  val prisoner: Prisoner
)

data class Prisoner(
  val prisonerNumber: String? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val status: String? = null,
)

@Suppress("unused")
enum class MatchedBy {
  ALL_SUPPLIED,
  ALL_SUPPLIED_ALIAS,
  HMPPS_KEY,
  EXTERNAL_KEY,
  NAME,
  PARTIAL_NAME,
  PARTIAL_NAME_DOB_LENIENT,
  NOTHING
}

data class PrisonerSearchRequest(val prisonerIdentifier: String)

data class PrisonerMatchRequest(val nomsNumber: String)

data class SearchRequest(val nomsNumber: String)

data class SearchResult(
  val firstName: String?,
  val lastName: String?,
  val nomsNumber: String?,
  val dateOfBirth: LocalDate? = null,
)
