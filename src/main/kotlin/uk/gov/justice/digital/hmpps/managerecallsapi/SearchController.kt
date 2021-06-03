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
import java.time.LocalDate

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SearchController(
//  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
//  @Autowired private val probationOffenderSearchClient: ProbationOffenderSearchClient
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  data class SearchRequest(val name: String)

  data class SearchResult(
    val firstName: String?,
    val lastName: String?,
    val nomisNumber: String?,
    val dateOfBirth: LocalDate? = null,
    val aliases: List<SearchAlias> = emptyList()
  )

  data class SearchAlias(
    val firstName: String? = null,
    val middleNames: String? = null,
    val lastName: String? = null,
    val dateOfBirth: LocalDate? = null
  )

  @PostMapping("/search")
  @ResponseBody
  fun prisonerSearch(@RequestBody searchRequest: SearchRequest) =
    ResponseEntity.ok(
      listOf(SearchResult("Bob", "Smith", "blah"))
    )}
