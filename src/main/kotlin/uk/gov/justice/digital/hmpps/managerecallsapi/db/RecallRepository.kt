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
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import java.util.UUID
import javax.transaction.Transactional

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
  fun addDocumentToRecall(recallId: RecallId, recallDocument: RecallDocument)
}

@Component
class RecallRepository(
  @Qualifier("jpaRecallRepository") @Autowired private val jpaRepository: JpaRecallRepository
) : JpaRecallRepository by jpaRepository, ExtendedRecallRepository {
  override fun getByRecallId(recallId: RecallId): Recall =
    findByRecallId(recallId) ?: throw RecallNotFoundException(recallId)

  override fun search(searchRequest: RecallSearchRequest): List<Recall> =
    findByNomsNumber(searchRequest.nomsNumber) // Intention is to support a richer query object but use of QueryByExample has issues e.g. with non-nullable Recall.id

  override fun findByRecallId(recallId: RecallId): Recall? =
    findById(recallId.value).orElse(null)

  @Transactional
  override fun addDocumentToRecall(recallId: RecallId, recallDocument: RecallDocument) {
    getByRecallId(recallId).let { recall ->
      val updatedDocuments = recall.documents.toMutableSet().apply {
        this.removeIf { it.id == recallDocument.id }
      } + recallDocument
      save(recall.copy(documents = updatedDocuments))
    }
  }
}
