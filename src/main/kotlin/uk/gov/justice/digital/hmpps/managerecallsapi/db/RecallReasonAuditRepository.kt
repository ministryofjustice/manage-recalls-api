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

@Repository("jpaRecallReasonAuditRepository")
interface JpaRecallReasonAuditRepository : JpaRepository<RawRecallReasonAudit, Int> {

  @Query(
    value = """
      select 
        audit_id as auditId, 
        cast(recall_id as varchar) as recallId,
        concat(u.first_name , ' ', u.last_name) as updatedByUserName,
        updated_date_time as updatedDateTime, 
        cast(value_list as varchar) as updatedValue
      from recall_reason_audit a,
      user_details u 
      where 
          a.updated_by_user_id = u.id
          and audit_id in (
          select max(audit_id) from recall_reason_audit
          where recall_id = :recallId
          group by updated_date_time) 
      """,
    nativeQuery = true
  )
  fun auditDetailsForRecallId(recallId: UUID): List<RecallFieldAudit>

  @Query(
    value = """
      select columnName, auditId, a.updated_date_time as updatedDateTime, auditCount,
       concat(u.first_name , ' ', u.last_name) as updatedByUserName
         from (
                  select max(audit_id)          as auditId,
                         'reasons_for_recall'   as columnName,
                         count(*)               as auditCount
                  from recall_reason_audit a
                  where audit_id in (
                      select max(audit_id) as audit_id
                      from recall_reason_audit
                      where recall_id = :recallId
                      group by updated_date_time
                  )
              ) grouped_audit,
              recall_reason_audit a,
              user_details u
        where a.updated_by_user_id = u.id
          and a.audit_id = auditId """,
    nativeQuery = true
  )
  fun auditSummaryForRecallId(recallId: UUID): RecallFieldSummary?
}

@NoRepositoryBean
interface ExtendedRecallReasonAuditRepository : JpaRecallReasonAuditRepository

@Component
class RecallReasonAuditRepository(
  @Qualifier("jpaRecallReasonAuditRepository") @Autowired private val jpaRepository: JpaRecallReasonAuditRepository
) : JpaRecallReasonAuditRepository by jpaRepository, ExtendedRecallReasonAuditRepository

// Used only for schema-validation
@Entity
@Table(name = "recall_reason_audit")
data class RawRecallReasonAudit(
  @Id
  val auditId: Int,
  val recallId: UUID,
  val updatedByUserId: UUID,
  val updatedDateTime: OffsetDateTime,
  val updatedValue: String,
  val query: String
)
