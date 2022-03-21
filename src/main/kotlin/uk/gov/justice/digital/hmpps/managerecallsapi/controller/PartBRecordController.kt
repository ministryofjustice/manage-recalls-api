package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PartBRecordService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.VirusFoundException
import java.time.LocalDate

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class PartBRecordController(
  @Autowired private val partBRecordService: PartBRecordService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @PostMapping("/recalls/{recallId}/partb-records")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPartBRecord(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: PartBRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): PartBRecordId =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      partBRecordService.createRecord(recallId, currentUserId, request).map {
        it
      }.onFailure {
        throw VirusFoundException()
      }
    }

  data class PartBRequest(
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
