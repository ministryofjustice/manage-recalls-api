@file:Suppress("SpringDataMethodInconsistencyInspection")

package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.util.UUID

@Repository("jpaRecallRepository")
interface JpaRecallRepository : JpaRepository<Recall, UUID>

@NoRepositoryBean
interface ExtendedRecallRepository : JpaRecallRepository {
  fun getByRecallId(recallId: RecallId): Recall
}

@Component
class RecallRepository(
  @Qualifier("jpaRecallRepository") @Autowired private val jpaRepository: JpaRecallRepository
) : JpaRecallRepository by jpaRepository, ExtendedRecallRepository {
  @Transactional
  override fun getByRecallId(recallId: RecallId): Recall = getById(recallId.value)
}
