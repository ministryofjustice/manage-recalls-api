package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopReason
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.StopRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.Clock
import java.time.OffsetDateTime
import javax.transaction.Transactional

@Service
class StatusService(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val clock: Clock
) {

  @Transactional
  fun stopRecall(recallId: RecallId, stopReason: StopReason, currentUserId: UserId) {
    recallRepository.getByRecallId(recallId).let { recall ->
      val stopRecord = StopRecord(stopReason, currentUserId, OffsetDateTime.now(clock))
      recallRepository.save(recall.copy(stopRecord = stopRecord), currentUserId)
    }
  }
}
