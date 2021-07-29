@file:Suppress("SpringDataMethodInconsistencyInspection")

package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
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
  override fun getByRecallId(recallId: RecallId): Recall =
    try {
      getById(recallId.value)
    } catch (e: JpaObjectRetrievalFailureException) {
      throw RecallNotFoundException("Recall not found: '$recallId'", e)
    }
}
