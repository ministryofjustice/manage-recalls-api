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

@Repository("jpaUnversionedDocumentRepository")
interface JpaUnversionedDocumentRepository : JpaRepository<UnversionedDocument, UUID> {
  @Query("SELECT d from UnversionedDocument d where d.recallId = :recallId and d.id = :documentId")
  fun findByRecallIdAndDocumentId(
    @Param("recallId") recallId: UUID,
    @Param("documentId") documentId: UUID
  ): UnversionedDocument?
}

@NoRepositoryBean
interface ExtendedUnversionedDocumentRepository : JpaUnversionedDocumentRepository {
  fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: UUID): UnversionedDocument
}

@Component
class UnversionedDocumentRepository(
  @Qualifier("jpaUnversionedDocumentRepository") @Autowired private val jpaRepository: JpaUnversionedDocumentRepository
) : JpaUnversionedDocumentRepository by jpaRepository, ExtendedUnversionedDocumentRepository {
  override fun getByRecallIdAndDocumentId(recallId: RecallId, documentId: UUID) =
    findByRecallIdAndDocumentId(recallId.value, documentId)
      ?: throw RecallDocumentNotFoundException(recallId, documentId)
}
