package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.config.VirusFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RescindRecordService
import java.time.LocalDate

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RescindRecordController(
  @Autowired private val rescindRecordService: RescindRecordService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @PostMapping("/recalls/{recallId}/rescind-records")
  @Operation(summary = "Creates a new rescind record for the recall identified by the recallId")
  @ResponseStatus(HttpStatus.CREATED)
  fun createRescindRecord(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: RescindRequestRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): RescindRecordId =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      rescindRecordService.createRecord(recallId, currentUserId, request).map {
        it
      }.onFailure {
        throw VirusFoundException()
      }
    }

  @PatchMapping("/recalls/{recallId}/rescind-records/{rescindRecordId}")
  @Operation(summary = "Adds decision information to the open rescind record for the recall identified by the recallId")
  fun decideRescindRecord(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("rescindRecordId") rescindRecordId: RescindRecordId,
    @RequestBody request: RescindDecisionRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): RescindRecordId =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      rescindRecordService.makeDecision(recallId, currentUserId, rescindRecordId, request).map {
        it
      }.onFailure {
        throw VirusFoundException()
      }
    }

  data class RescindRequestRequest(
    val details: String,
    val emailReceivedDate: LocalDate,
    val emailFileContent: String,
    val emailFileName: FileName,
  )

  data class RescindDecisionRequest(
    val approved: Boolean,
    val details: String,
    val emailSentDate: LocalDate,
    val emailFileContent: String,
    val emailFileName: FileName,
  )
}
