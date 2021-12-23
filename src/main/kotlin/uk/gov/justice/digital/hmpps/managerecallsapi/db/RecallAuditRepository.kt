package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository("jpaRecallAuditRepository")
interface JpaRecallAuditRepository : JpaRepository<RawRecallAudit, UUID> {

  @Query(
    value = "select audit_id as auditId, cast(recall_id as varchar) as recallId, cast(updated_by_user_id as varchar) as updatedByUserId, action_timestamp as actionTimestamp, " +
      " updated_values ->> :columnName as updatedValue" +
      "    from recall_audit where recall_id = :recallId " +
      " and jsonb_exists(cast(updated_values as jsonb), :columnName)",
    nativeQuery = true
  )
  fun auditFoRecallIdAndColumnName(recallId: UUID, columnName: String): List<RecallAudit>
}

@NoRepositoryBean
interface ExtendedRecallAuditRepository : JpaRecallAuditRepository

@Component
class RecallAuditRepository(
  @Qualifier("jpaRecallAuditRepository") @Autowired private val jpaRepository: JpaRecallAuditRepository
) : JpaRecallAuditRepository by jpaRepository, ExtendedRecallAuditRepository

interface RecallAudit {
  val auditId: Int
  val recallId: UUID
  val updatedByUserId: UUID
  val actionTimestamp: OffsetDateTime
  val updatedValue: String
}

@Entity
@Table(name = "recall_audit")
data class RawRecallAudit(
  @Id
  val auditId: Int,
  val recallId: UUID,
  val updatedByUserId: UUID,
  val actionTimestamp: OffsetDateTime
)
