package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.lang.UnsupportedOperationException
import java.util.UUID

@Repository("jpaRecallRepository")
interface JpaRecallRepository : JpaRepository<Recall, UUID> {
  @Query("SELECT r from Recall r where r.nomsNumber = :nomsNumber")
  fun findByNomsNumber(@Param("nomsNumber") nomsNumber: NomsNumber): List<Recall>
}

@NoRepositoryBean
interface ExtendedRecallRepository : JpaRecallRepository {
  fun getByRecallId(recallId: RecallId): Recall
  fun search(searchRequest: RecallSearchRequest): List<Recall>
  fun findByRecallId(recallId: RecallId): Recall?
}

// Needed to prevent access to saving directly
class RawRecallRepository(@Qualifier("jpaRecallRepository") @Autowired private val jpaRepository: JpaRecallRepository) : JpaRecallRepository by jpaRepository, ExtendedRecallRepository {
  override fun getByRecallId(recallId: RecallId): Recall =
    findByRecallId(recallId) ?: throw RecallNotFoundException(recallId)

  override fun search(searchRequest: RecallSearchRequest): List<Recall> =
    findByNomsNumber(searchRequest.nomsNumber) // Intention is to support a richer query object but use of QueryByExample has issues e.g. with non-nullable Recall.id

  override fun findByRecallId(recallId: RecallId): Recall? =
    findById(recallId.value).orElse(null)
}

@Component
class RecallRepository(
  @Qualifier("jpaRecallRepository") @Autowired private val jpaRepository: JpaRecallRepository
) : RawRecallRepository(jpaRepository) {

  @Deprecated(message = "Must be saved with lastUpdatedByUserId", replaceWith = ReplaceWith("save(entity, currentUserId)"), level = DeprecationLevel.ERROR)
  override fun <S : Recall?> save(entity: S): S {
    throw UnsupportedOperationException("Only save with userId to ensure lastUpdatedByUserId is correct")
  }

  fun save(entity: Recall, currentUserId: UserId) =
    super.save(entity.copy(lastUpdatedByUserId = currentUserId.value))
}
