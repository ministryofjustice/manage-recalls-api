package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.config.MissingPhaseStartException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.PhaseRecordRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhaseRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
class PhaseRecordService(
  @Autowired private val phaseRecordRepository: PhaseRecordRepository,
) {

  @Transactional
  fun startPhase(recallId: RecallId, phase: Phase, currentUserId: UserId): PhaseRecord {
    val phaseRecord = phaseRecordRepository.findByRecallIdAndPhase(recallId.value, phase)?.copy(
      startedDateTime = OffsetDateTime.now(),
      startedByUserId = currentUserId.value,
    ) ?: PhaseRecord(
      ::PhaseRecordId.random(),
      recallId,
      phase,
      currentUserId,
      OffsetDateTime.now()
    )
    return phaseRecordRepository.save(phaseRecord)
  }

  @Transactional
  fun endPhase(recallId: RecallId, phase: Phase, currentUserId: UserId): PhaseRecord {
    val phaseRecord = phaseRecordRepository.findByRecallIdAndPhase(recallId.value, phase) ?: throw MissingPhaseStartException(recallId, phase)

    val updatedPhaseRecord = phaseRecord.copy(
      endedDateTime = OffsetDateTime.now(),
      endedByUserId = currentUserId.value,
    )

    return phaseRecordRepository.save(updatedPhaseRecord)
  }
}
