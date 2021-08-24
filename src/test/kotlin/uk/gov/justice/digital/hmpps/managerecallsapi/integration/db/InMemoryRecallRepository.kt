package uk.gov.justice.digital.hmpps.managerecallsapi.integration.db
import io.mockk.mockk
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException

@Component
@Profile("test")
class InMemoryRecallRepository : RecallRepository by mockk() {
  private val recalls = mutableMapOf<RecallId, Recall>()

  override fun <S : Recall> save(recall: S): S {
    recalls[recall.recallId()] = recall
    return recall
  }

  override fun getByRecallId(recallId: RecallId): Recall {
    return recalls[recallId] ?: throw RecallNotFoundException("Recall not found: '$recallId'", Exception())
  }

  override fun findAll(): MutableList<Recall> = recalls.values.toMutableList()
}
