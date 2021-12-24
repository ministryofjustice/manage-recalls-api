package uk.gov.justice.digital.hmpps.managerecallsapi.db

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository("jpaRecallAuditRepository")
interface JpaRecallAuditRepository : JpaRepository<RawRecallAudit, UUID> {

  @Query(
    value = "select audit_id as auditId, cast(recall_id as varchar) as recallId, " +
      " concat(u.first_name , ' ', u.last_name) as updatedByUserName, " +
      " updated_date_time as updatedDateTime, " +
      " updated_values ->> :columnName as updatedValue" +
      "    from recall_audit a," +
      "    user_details u " +
      " where a.updated_by_user_id = u.id " +
      "  and recall_id = :recallId " +
      "  and jsonb_exists(cast(updated_values as jsonb), :columnName)",
    nativeQuery = true
  )
  fun auditForRecallIdAndColumnName(recallId: UUID, columnName: String): List<RecallFieldAudit>
}

@NoRepositoryBean
interface ExtendedRecallAuditRepository : JpaRecallAuditRepository

@Component
class RecallAuditRepository(
  @Qualifier("jpaRecallAuditRepository") @Autowired private val jpaRepository: JpaRecallAuditRepository
) : JpaRecallAuditRepository by jpaRepository, ExtendedRecallAuditRepository

interface RecallFieldAudit {
  val auditId: Int
  val recallId: UUID
  val updatedByUserName: String
  val updatedDateTime: Timestamp
  val updatedValue: String
}

// Used only for schema-validation
@Entity
@Table(name = "recall_audit")
data class RawRecallAudit(
  @Id
  val auditId: Int,
  val recallId: UUID,
  val updatedByUserId: UUID,
  val updatedDateTime: OffsetDateTime
)
