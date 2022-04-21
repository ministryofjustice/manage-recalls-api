package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.swagger.v3.oas.annotations.Operation
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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import java.time.OffsetDateTime
import javax.transaction.Transactional

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class StatusController(
  @Autowired private val recallService: RecallService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @PostMapping("/recalls/{recallId}/returned-to-custody")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Updates information on the recall associated with the offender returning to custody")
  fun returnedToCustody(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: ReturnedToCustodyRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): Unit =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      recallService.manuallyReturnedToCustody(recallId, request.returnedToCustodyDateTime, request.returnedToCustodyNotificationDateTime, currentUserId)
    }

  @PostMapping("/recalls/{recallId}/stop")
  @Transactional
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Marks the recall identified by the recallId as stopped")
  fun stopRecall(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: StopRecallRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): Unit =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      recallService.stopRecall(recallId, request, currentUserId)
    }
}

data class ReturnedToCustodyRequest(
  val returnedToCustodyDateTime: OffsetDateTime,
  val returnedToCustodyNotificationDateTime: OffsetDateTime,
)

data class StopRecallRequest(
  val stopReason: StopReason,
)
