package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.hibernate.SessionFactory
import org.hibernate.annotations.common.util.StringHelper.booleanValue
import org.hibernate.persister.entity.AbstractEntityPersister
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.FieldAuditEntry
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.FieldAuditSummary
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallAuditRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallFieldAudit
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallFieldSummary
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.ColumnName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.persistence.Column
import javax.persistence.EntityManagerFactory

@Service
class RecallAuditService(
  @Autowired private val recallAuditRepository: RecallAuditRepository,
  @Autowired private val entityManagerFactory: EntityManagerFactory
) {
  val sessionFactory: SessionFactory = entityManagerFactory.unwrap(SessionFactory::class.java)
  val persister = sessionFactory.getClassMetadata(Recall::class.java) as AbstractEntityPersister

  fun getAuditForFieldName(recallId: RecallId, fieldName: FieldName): List<FieldAuditEntry> {
    val columnName = getColumnNameForField(fieldName)
    return recallAuditRepository.auditForRecallIdAndColumnName(recallId.value, columnName.value)
      .map { it.toFieldAuditEntry(fieldName) }
  }

  fun getAuditSummaryForRecall(recallId: RecallId): List<FieldAuditSummary> {
    return recallAuditRepository.auditSummaryForRecall(recallId.value)
      .map { it.toFieldAuditSummary() }
  }

  private fun RecallFieldAudit.toFieldAuditEntry(fieldName: FieldName) =
    FieldAuditEntry(
      this.auditId,
      RecallId(this.recallId),
      FullName(this.updatedByUserName),
      this.updatedDateTime.toLocalDateTime().atOffset(ZoneOffset.UTC),
      translateValue(this.updatedValue, fieldName)
    )

  private fun RecallFieldSummary.toFieldAuditSummary() =
    FieldAuditSummary(
      this.auditId,
      getFieldNameForColumn(ColumnName(this.columnName)),
      FullName(this.updatedByUserName),
      this.updatedDateTime.toLocalDateTime().atOffset(ZoneOffset.UTC),
      this.auditCount
    )

  private fun getColumnNameForField(fieldName: FieldName): ColumnName {
    val (recallField, fieldPath) = getRecallFieldAndPath(fieldName)
    return ColumnName(
      if (
        recallField.isAnnotationPresent(Column::class.java) &&
        StringUtils.hasText(recallField.getAnnotation(Column::class.java).name)
      ) {
        recallField.getAnnotation(Column::class.java).name
      } else {
        if (persister.getSubclassPropertyColumnNames(fieldPath).isNotEmpty()) {
          persister.getSubclassPropertyColumnNames(fieldPath)
        } else {
          persister.getPropertyColumnNames(fieldName.getNameFromPath())
        }[0]
      }
    )
  }

  private fun getRecallFieldAndPath(fieldName: FieldName): Pair<Field, String> {
    val fieldPathParts = fieldName.value.split(".")
    var field = Recall::class.java.getDeclaredField(fieldPathParts[0])
    for (i in 1 until fieldPathParts.size) {
      field = field.type.getDeclaredField(fieldPathParts[i])
    }
    return Pair(field, fieldName.value)
  }

  private fun getFieldNameForColumn(columnName: ColumnName): FieldName {
    return Recall::class.java.declaredFields.first { field ->
      getColumnNameForField(FieldName(field.name)) == columnName
    }.let { FieldName(it.name) }
  }

  private fun FieldName.getNameFromPath(): String = this.value.split(".").last()

  private fun translateValue(updatedValue: String, fieldName: FieldName): Any =
    when (getRecallFieldAndPath(fieldName).first.type) {
      java.lang.Boolean::class.java -> booleanValue(updatedValue)
      LocalDate::class.java -> LocalDate.parse(updatedValue)
      OffsetDateTime::class.java -> OffsetDateTime.parse(updatedValue.replace(" ", "T"))
      UUID::class.java -> UUID.fromString(updatedValue)
      Int::class.java -> updatedValue.toInt()
      else -> updatedValue
    }
}
