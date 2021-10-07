package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentNotFoundException
import java.util.UUID

@Repository("jpaRecallDocumentRepository")
interface JpaRecallDocumentRepository : JpaRepository<RecallDocument, UUID> {
  @Query("SELECT d from RecallDocument d where d.recallId = :recallId and d.category = :category")
  fun findByRecallIdAndCategory(
    @Param("recallId") recallId: UUID,
    @Param("category") category: RecallDocumentCategory
  ): RecallDocument?

  @Query("SELECT d from RecallDocument d where d.recallId = :recallId and d.id = :documentId")
  fun findByRecallIdAndDocumentId(
    @Param("recallId") recallId: UUID,
    @Param("documentId") documentId: UUID
  ): RecallDocument?
}

@NoRepositoryBean
interface ExtendedRecallDocumentRepository : JpaRecallDocumentRepository {
  fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: UUID): RecallDocument
}

@Component
class RecallDocumentRepository(
  @Qualifier("jpaRecallDocumentRepository") @Autowired private val jpaRepository: JpaRecallDocumentRepository
) : JpaRecallDocumentRepository by jpaRepository, ExtendedRecallDocumentRepository {
  override fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: UUID) =
    findByRecallIdAndDocumentId(recallId.value, documentId)
      ?: throw RecallDocumentNotFoundException(recallId, documentId)
}
