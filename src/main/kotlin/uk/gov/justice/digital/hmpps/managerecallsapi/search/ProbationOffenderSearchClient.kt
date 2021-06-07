package uk.gov.justice.digital.hmpps.managerecallsapi.search

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ProbationOffenderSearchClient {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Autowired
  @Qualifier("probationOffenderSearchWebClient")
  internal lateinit var webClient: AuthenticatingRestClient

  fun search(searchRequest: SearchRequest): List<SearchResult> {
    log.info("Sending probation search request: $searchRequest")

    return webClient
      .post("/match", OffenderMatchRequest(null, searchRequest.name))
      .retrieve()
      .bodyToMono(OffenderMatches::class.java)
      .block()!!
      .toSearchResults()
  }
}

fun OffenderMatches?.toSearchResults() =
  this?.let {
    matches.map {
      with(it.offender) {
        val searchAliases =
          aliases?.map { SearchAlias(it.firstName, it.middleNames?.joinToString(" "), it.lastName, it.dateOfBirth) }
            .orEmpty()
        SearchResult(firstName, lastName, otherIds?.nomsNumber, dateOfBirth, searchAliases)
      }
    }
  }.orEmpty()

data class OffenderMatchRequest(val firstName: String?, val surname: String)

data class OffenderMatches(
  val matches: List<OffenderMatch> = listOf(),
  val matchedBy: MatchedBy = MatchedBy.NOTHING
)

data class OffenderMatch(
  val offender: Offender
)

data class Offender(
  val offenderId: Long,
  val otherIds: OtherIds? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val status: String? = null,
  val aliases: List<OffenderAlias>? = null
)

data class OffenderAlias(
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null
)

data class OtherIds(
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val niNumber: String? = null,
  val nomsNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonerNumber: String? = null
)
