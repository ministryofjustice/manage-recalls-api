package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentNotFoundException
import java.util.UUID

@Repository("jpaDocumentRepository")
interface JpaDocumentRepository : JpaRepository<Document, UUID> {
  @Query("SELECT d from Document d where d.recallId = :recallId and d.category = :category")
  fun findByRecallIdAndCategory(
    @Param("recallId") recallId: UUID,
    @Param("category") category: RecallDocumentCategory
  ): Document?

  @Query("SELECT d from Document d where d.recallId = :recallId and d.id = :documentId")
  fun findByRecallIdAndDocumentId(
    @Param("recallId") recallId: UUID,
    @Param("documentId") documentId: UUID
  ): Document?
}

@NoRepositoryBean
interface ExtendedDocumentRepository : JpaDocumentRepository {
  fun findByRecallIdAndDocumentId(recallId: RecallId, documentId: DocumentId): Document?
  fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: DocumentId): Document
}

@Component
class DocumentRepository(
  @Qualifier("jpaDocumentRepository") @Autowired private val jpaRepository: JpaDocumentRepository
) : JpaDocumentRepository by jpaRepository, ExtendedDocumentRepository {
  override fun findByRecallIdAndDocumentId(recallId: RecallId, documentId: DocumentId): Document? =
    findByRecallIdAndDocumentId(recallId.value, documentId.value)

  override fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: DocumentId) =
    findByRecallIdAndDocumentId(recallId, documentId)
      ?: throw RecallDocumentNotFoundException(recallId, documentId)
}
