package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.config.LastKnownAddressNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddressRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class LastKnownAddressController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val lastKnownAddressRepository: LastKnownAddressRepository,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @ApiResponses(
    ApiResponse(
      responseCode = "404",
      description = "RecallNotFoundException(recallId=...)",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
    )
  )
  @PostMapping(
    "/recalls/{recallId}/last-known-addresses"
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun createLastKnownAddress( // TODO: PUD-1500: should be moved into a service class and annotated @Transactional
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: CreateLastKnownAddressRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): LastKnownAddressId {
    return recallRepository.getByRecallId(recallId).let { recall ->
      val currentUserId = tokenExtractor.getTokenFromHeader(bearerToken).userUuid()
      val previousIndex = recall.lastKnownAddresses.maxByOrNull { it.index }?.index ?: 0
      val saved = lastKnownAddressRepository.save(
        LastKnownAddress(
          ::LastKnownAddressId.random(),
          recallId,
          request.line1,
          request.line2,
          request.town,
          request.postcode,
          request.source,
          previousIndex + 1,
          currentUserId,
          OffsetDateTime.now()
        )
      )
      saved.id()
    }
  }

  @Throws(LastKnownAddressNotFoundException::class)
  @DeleteMapping("recalls/{recallId}/last-known-addresses/{lastKnownAddressId}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  fun deleteAddress(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("lastKnownAddressId") lastKnownAddressId: LastKnownAddressId
  ) {
    lastKnownAddressRepository.deleteByRecallIdAndLastKnownAddressId(recallId, lastKnownAddressId)
  }
}

data class CreateLastKnownAddressRequest(
  val line1: String,
  val line2: String?,
  val town: String,
  val postcode: String?,
  val source: AddressSource,
)
