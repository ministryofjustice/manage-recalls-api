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
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
@Validated
class SearchController(@Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient) {

  @PostMapping("/search")
  fun prisonerSearch(@Valid @RequestBody searchRequest: SearchRequest): Mono<ResponseEntity<List<SearchResult>>> =
    prisonerOffenderSearchClient.prisonerSearch(searchRequest)
      .map { ResponseEntity.ok(it.toSearchResults()) }
}

fun List<Prisoner>?.toSearchResults() =
  this?.let { prisoners ->
    prisoners.map {
      SearchResult(it.firstName, it.lastName, it.prisonerNumber, it.dateOfBirth)
    }
  }.orEmpty()

data class SearchRequest(@field:NotEmpty val nomsNumber: String)
data class SearchResult(
  val firstName: String?,
  val lastName: String?,
  val nomsNumber: String?,
  val dateOfBirth: LocalDate? = null,
)
