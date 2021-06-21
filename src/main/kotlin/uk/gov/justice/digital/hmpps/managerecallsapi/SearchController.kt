package uk.gov.justice.digital.hmpps.managerecallsapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.LocalDate

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class SearchController(
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/search")
  @ResponseBody
  fun prisonerSearch(@RequestBody searchRequest: SearchRequest) =
    ResponseEntity.ok(
      prisonerOffenderSearchClient.prisonerSearch(searchRequest).toSearchResults()
    )
}

fun List<Prisoner>?.toSearchResults() =
  this?.let {
    this.map {
      SearchResult(it.firstName, it.lastName, it.prisonerNumber, it.dateOfBirth)
    }
  }.orEmpty()

data class SearchRequest(val nomsNumber: String)
data class SearchResult(
  val firstName: String?,
  val lastName: String?,
  val nomsNumber: String?,
  val dateOfBirth: LocalDate? = null,
)
