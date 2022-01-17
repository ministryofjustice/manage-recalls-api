package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
class SearchController(@Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient) {

  @PostMapping("/search")
  fun prisonerSearch(@RequestBody searchRequest: SearchRequest): Mono<ResponseEntity<List<Api.Prisoner>>> =
    prisonerOffenderSearchClient.prisonerSearch(searchRequest)
      .map { ResponseEntity.ok(it.toApiPrisoners()) }

  @GetMapping("/prisoner/{nomsNumber}")
  fun prisonerByNomsNumber(@PathVariable("nomsNumber") nomsNumber: NomsNumber): Mono<ResponseEntity<Api.Prisoner>> =
    prisonerOffenderSearchClient.prisonerByNomsNumber(nomsNumber)
      .map { ResponseEntity.ok(it.toApiPrisoner()) }

  class Api {
    data class Prisoner(
      val firstName: String?,
      val middleNames: String?,
      val lastName: String?,
      val dateOfBirth: LocalDate?,
      val gender: String?,
      val nomsNumber: String?,
      val pncNumber: String?,
      val croNumber: String?,
    )
  }

  fun List<Prisoner>?.toApiPrisoners() =
    this?.let { prisoners ->
      prisoners.map {
        it.toApiPrisoner()
      }
    }.orEmpty()

  private fun Prisoner.toApiPrisoner() =
    Api.Prisoner(
      this.firstName,
      this.middleNames,
      this.lastName,
      this.dateOfBirth,
      this.gender,
      this.prisonerNumber,
      this.pncNumber,
      this.croNumber
    )
}

data class SearchRequest(val nomsNumber: NomsNumber)
