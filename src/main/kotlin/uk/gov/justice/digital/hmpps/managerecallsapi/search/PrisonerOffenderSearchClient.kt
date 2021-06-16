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

  fun search(searchRequest: SearchRequest): List<SearchResult> {
    log.info("Sending prisoner search request: $searchRequest")

    return webClient
      .post("/match-prisoners", PrisonerMatchRequest(null, searchRequest.name))
      .retrieve()
      .bodyToMono(PrisonerMatches::class.java)
      .block()!!
      .toSearchResults()
  }
}

fun PrisonerMatches?.toSearchResults() =
  this?.let {
    matches.map {
      with(it.prisoner) {
        SearchResult(firstName!!, lastName!!, prisonerNumber, dateOfBirth)
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

data class PrisonerMatchRequest(val firstName: String?, val lastName: String)

data class SearchRequest(val name: String)

data class SearchResult(
  val firstName: String?,
  val lastName: String?,
  val nomisNumber: String?,
  val dateOfBirth: LocalDate? = null,
)
