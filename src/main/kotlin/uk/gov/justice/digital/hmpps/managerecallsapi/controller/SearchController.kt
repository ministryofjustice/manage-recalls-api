package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Api.Prisoner::class))]
      )
    ]
  )
  @GetMapping("/prisoner/{nomsNumber}")
  fun prisonerByNomsNumber(@PathVariable("nomsNumber") nomsNumber: NomsNumber): Mono<Api.Prisoner> =
    prisonerOffenderSearchClient.prisonerByNomsNumber(nomsNumber)
      .map { it.toApiPrisoner() }

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
