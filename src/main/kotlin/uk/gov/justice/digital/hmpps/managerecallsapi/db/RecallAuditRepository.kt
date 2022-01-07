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
    value = """
      select audit_id as auditId, cast(recall_id as varchar) as recallId, 
       concat(u.first_name , ' ', u.last_name) as updatedByUserName, 
       updated_date_time as updatedDateTime, 
       updated_values ->> :columnName as updatedValue
          from recall_audit a,
          user_details u 
       where a.updated_by_user_id = u.id 
        and recall_id = :recallId 
        and jsonb_exists(cast(updated_values as jsonb), :columnName)""",
    nativeQuery = true
  )
  fun auditForRecallIdAndColumnName(recallId: UUID, columnName: String): List<RecallFieldAudit>

  @Query(
    value = """
      select column_name as columnName, auditId, updatedDateTime,
        concat(u.first_name , ' ', u.last_name) as updatedByUserName,
        auditCount
        from ( 
          select max(updated_date_time) as updatedDateTime, max(audit_id) as auditId, column_name, count(*) as auditCount from ( 
              select audit_id, updated_date_time, json_object_keys(updated_values) as column_name 
              from recall_audit where recall_id = :recallId) as audit_by_field 
          group by column_name) grouped_audit, 
           recall_audit a,
           user_details u 
          where a.updated_by_user_id = u.id 
            and a.audit_id = auditId """,
    nativeQuery = true
  )
  fun auditSummaryForRecall(recallId: UUID): List<RecallFieldSummary>
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
interface RecallFieldSummary {
  val columnName: String
  val auditId: Int
  val updatedByUserName: String
  val updatedDateTime: Timestamp
  val auditCount: Int
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
