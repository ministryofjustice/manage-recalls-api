package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.LocalDate

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
@Validated
class SearchController(@Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient) {

  @PostMapping("/search")
  fun prisonerSearch(@RequestBody searchRequest: SearchRequest): Mono<ResponseEntity<List<SearchResult>>> =
    prisonerOffenderSearchClient.prisonerSearch(searchRequest)
      .map { ResponseEntity.ok(it.toSearchResults()) }
}

fun List<Prisoner>?.toSearchResults() =
  this?.let { prisoners ->
    prisoners.map {
      SearchResult(
        it.firstName,
        it.middleNames,
        it.lastName,
        it.dateOfBirth,
        it.gender,
        it.prisonerNumber,
        it.pncNumber,
        it.croNumber
      )
    }
  }.orEmpty()

data class SearchRequest(val nomsNumber: NomsNumber)
data class SearchResult(
  val firstName: String?,
  val middleNames: String?,
  val lastName: String?,
  val dateOfBirth: LocalDate?,
  val gender: String?,
  val nomsNumber: String?,
  val pncNumber: String?,
  val croNumber: String?,
)
