package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentNotFoundException
import java.util.UUID

@Repository("jpaDocumentRepository")
interface JpaDocumentRepository : JpaRepository<Document, UUID> {
  // TODO: don't use with unversioned categories! This needs to evolve to e.g. getLatest or to return List<Document> or similar.
  fun findFirstByRecallIdAndCategoryOrderByVersionDesc(
    @Param("recallId") recallId: UUID,
    @Param("category") category: DocumentCategory
  ): Document?

  fun findByIdAndRecallId(
    @Param("documentId") documentId: UUID,
    @Param("recallId") recallId: UUID
  ): Document?

  fun getAllByRecallIdAndCategory(
    @Param("recallId") recallId: UUID,
    @Param("category") category: DocumentCategory
  ): List<Document>

  fun findByRecallIdAndCategoryAndVersion(
    @Param("recallId") recallId: UUID,
    @Param("category") category: DocumentCategory,
    @Param("version") version: Int
  ): Document?
}

@NoRepositoryBean
interface ExtendedDocumentRepository : JpaDocumentRepository {
  fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: DocumentId): Document
  fun deleteByDocumentId(documentId: DocumentId)
  fun getAllByRecallIdAndCategory(recallId: RecallId, category: DocumentCategory): List<Document>
}

@Component
class DocumentRepository(
  @Qualifier("jpaDocumentRepository") @Autowired private val jpaRepository: JpaDocumentRepository
) : JpaDocumentRepository by jpaRepository, ExtendedDocumentRepository {

  override fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: DocumentId) =
    findByIdAndRecallId(documentId.value, recallId.value)
      ?: throw DocumentNotFoundException(recallId, documentId)

  override fun deleteByDocumentId(documentId: DocumentId) = deleteById(documentId.value)

  fun findLatestVersionedDocumentByRecallIdAndCategory(recallId: RecallId, category: DocumentCategory): Document? {
    if (!category.versioned) {
      throw WrongDocumentTypeException(category)
    }
    return findFirstByRecallIdAndCategoryOrderByVersionDesc(recallId.value, category)
  }

  override fun getAllByRecallIdAndCategory(recallId: RecallId, category: DocumentCategory): List<Document> {
    return getAllByRecallIdAndCategory(recallId.value, category)
  }
}
