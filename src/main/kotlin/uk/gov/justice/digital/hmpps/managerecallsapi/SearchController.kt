package uk.gov.justice.digital.hmpps.managerecallsapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.search.ProbationOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.search.SearchRequest

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SearchController(
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val probationOffenderSearchClient: ProbationOffenderSearchClient
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping("/search")
  @ResponseBody
  fun prisonerSearch(@RequestBody searchRequest: SearchRequest) =
    ResponseEntity.ok(
      prisonerOffenderSearchClient.search(searchRequest) + probationOffenderSearchClient.search(searchRequest)
    )
}
