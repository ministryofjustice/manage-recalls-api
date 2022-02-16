package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.BankHolidayService
import java.time.Clock
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class StatusController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val bankHolidayService: BankHolidayService,
  @Autowired private val tokenExtractor: TokenExtractor,
  @Autowired private val clock: Clock
) {

  @PostMapping("/recalls/{recallId}/returned-to-custody")
  fun returnedToCustody(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: ReturnedToCustodyRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): ResponseEntity<RecallResponse> =
    recallRepository.getByRecallId(recallId).let { recall ->
      val currentUserId = tokenExtractor.getTokenFromHeader(bearerToken).userUuid()
      recallRepository.save(
        recall.copy(
          returnedToCustody = ReturnedToCustodyRecord(
            request.returnedToCustodyDateTime,
            request.returnedToCustodyNotificationDateTime,
            currentUserId,
            OffsetDateTime.now(clock)
          ),
          dossierTargetDate = bankHolidayService.nextWorkingDate(request.returnedToCustodyNotificationDateTime.toLocalDate())
        ),
        currentUserId
      ).let {
        ResponseEntity.ok().build()
      }
    }
}

data class ReturnedToCustodyRequest(
  val returnedToCustodyDateTime: OffsetDateTime,
  val returnedToCustodyNotificationDateTime: OffsetDateTime,
)
