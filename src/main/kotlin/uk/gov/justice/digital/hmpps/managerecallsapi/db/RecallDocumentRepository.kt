package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository("jpaRecallDocumentRepository")
interface RecallDocumentRepository : JpaRepository<RecallDocument, UUID> {
  @Query("SELECT d from RecallDocument d where d.recallId = :recallId and d.category = :category")
  fun findByRecallIdAndCategory(
    @Param("recallId") recallId: UUID,
    @Param("category") category: RecallDocumentCategory
  ): RecallDocument?
}
