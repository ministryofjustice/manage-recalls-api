package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.config.MultiErrorResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.config.MultiFileException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PartBRecordService
import java.time.LocalDate

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class PartBRecordController(
  @Autowired private val partBRecordService: PartBRecordService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  // TODO: Persuade swagger to surface examples of both the 400 response objects
  @ApiResponses(
    ApiResponse(
      responseCode = "400",
      description = "Bad request, e.g. one or more files uploaded failed virus scanning",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(oneOf = [MultiErrorResponse::class, ErrorResponse::class])
        )
      ]
    )
  )
  @PostMapping("/recalls/{recallId}/partb-records")
  @Operation(summary = "Creates a Part B record for the recall identified by the recallId")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPartBRecord(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: PartBRecordRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): PartBRecordId =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      partBRecordService.createRecord(recallId, currentUserId, request).map {
        it
      }.recover {
        throw MultiFileException("VirusFoundException", it)
      }
    }

  data class PartBRecordRequest(
    val details: String,
    val partBReceivedDate: LocalDate,
    val partBFileName: FileName,
    val partBFileContent: String,
    val emailFileName: FileName,
    val emailFileContent: String,
    val oasysFileName: FileName?,
    val oasysFileContent: String?,
  )
}
