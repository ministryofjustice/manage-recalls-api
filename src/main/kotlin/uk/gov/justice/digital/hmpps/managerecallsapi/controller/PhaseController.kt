package uk.gov.justice.digital.hmpps.managerecallsapi.controller

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
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongPhaseStartException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PhaseRecordService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import javax.transaction.Transactional

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class PhaseController(
  @Autowired private val phaseRecordService: PhaseRecordService,
  @Autowired private val recallService: RecallService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @PostMapping("/recalls/{recallId}/start-phase")
  @Operation(summary = "Creates a Phase record for the supplied phase of the recall identified by the recallId")
  @ResponseStatus(HttpStatus.CREATED)
  fun startPhase(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: StartPhaseRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): PhaseRecord =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      val phase = request.phase
      if (phase == Phase.BOOK) {
        throw WrongPhaseStartException(recallId, phase)
      }
      phaseRecordService.startPhase(recallId, phase, currentUserId)
    }

  @PatchMapping("/recalls/{recallId}/end-phase")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Adds end information to the Phase record for the supplied phase of the recall identified by the recallId")
  @Transactional
  fun endPhase(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody request: EndPhaseRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): PhaseRecord =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      val phase = phaseRecordService.endPhase(recallId, request.phase, currentUserId)
      if (request.shouldUnassign) {
        recallService.unassignRecall(recallId, currentUserId)
      }
      return phase
    }
}

data class StartPhaseRequest(
  val phase: Phase
)

data class EndPhaseRequest(
  val phase: Phase,
  val shouldUnassign: Boolean
)

enum class Phase {
  BOOK,
  ASSESS,
  DOSSIER
}
