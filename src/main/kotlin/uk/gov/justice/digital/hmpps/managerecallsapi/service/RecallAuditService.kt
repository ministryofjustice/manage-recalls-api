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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldPath
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
  private val sessionFactory: SessionFactory = entityManagerFactory.unwrap(SessionFactory::class.java)
  private val persister = sessionFactory.getClassMetadata(Recall::class.java) as AbstractEntityPersister

  fun getAuditForFieldName(recallId: RecallId, fieldPath: FieldPath): List<FieldAuditEntry> {
    val columnName = getColumnNameForFieldPath(fieldPath)
    return recallAuditRepository.auditForRecallIdAndColumnName(recallId.value, columnName.value)
      .map { it.toFieldAuditEntry(fieldPath) }
  }

  fun getAuditSummaryForRecall(recallId: RecallId): List<FieldAuditSummary> {
    return recallAuditRepository.auditSummaryForRecall(recallId.value)
      .map { it.toFieldAuditSummary() }
  }

  private fun RecallFieldAudit.toFieldAuditEntry(fieldPath: FieldPath) =
    FieldAuditEntry(
      this.auditId,
      RecallId(this.recallId),
      FullName(this.updatedByUserName),
      this.updatedDateTime.toLocalDateTime().atOffset(ZoneOffset.UTC),
      translateValue(this.updatedValue, fieldPath)
    )

  private fun RecallFieldSummary.toFieldAuditSummary(): FieldAuditSummary {
    val fieldPath = getFieldPathForColumn(ColumnName(this.columnName))
    return FieldAuditSummary(
      this.auditId,
      fieldPath.getFieldName(),
      fieldPath,
      FullName(this.updatedByUserName),
      this.updatedDateTime.toLocalDateTime().atOffset(ZoneOffset.UTC),
      this.auditCount
    )
  }

  private fun getColumnNameForFieldPath(fieldPath: FieldPath): ColumnName {
    val recallField = getRecallField(fieldPath)
    return ColumnName(
      if (
        recallField.isAnnotationPresent(Column::class.java) &&
        StringUtils.hasText(recallField.getAnnotation(Column::class.java).name)
      ) {
        recallField.getAnnotation(Column::class.java).name
      } else {
        if (
          persister.getSubclassPropertyColumnNames(fieldPath.value) != null &&
          persister.getSubclassPropertyColumnNames(fieldPath.value).isNotEmpty()
        ) {
          persister.getSubclassPropertyColumnNames(fieldPath.value)
        } else {
          persister.getPropertyColumnNames(fieldPath.getFieldName().value)
        }[0]
      }
    )
  }

  private fun getRecallField(fieldPath: FieldPath): Field {
    val fieldPathParts = fieldPath.value.split(".")
    var field = Recall::class.java.getDeclaredField(fieldPathParts[0])
    for (i in 1 until fieldPathParts.size) {
      field = field.type.getDeclaredField(fieldPathParts[i])
    }
    return field
  }

  private fun getFieldPathForColumn(columnName: ColumnName): FieldPath =
    Recall::class.java.declaredFields.flatMap { field ->
      val propCols = persister.getPropertyColumnNames(field.name).map { ColumnName(it) to FieldPath(field.name) }
      if (propCols.size == 1) {
        propCols
      } else {
        field.type.declaredFields.flatMap { childField ->
          val childFieldPath = "${field.name}.${childField.name}"
          val childProbCols = persister.getPropertyColumnNames(childFieldPath)
          if (childProbCols.size == 1) {
            childProbCols.map { ColumnName(it) to FieldPath(childFieldPath) }
          } else {
            childField.type.declaredFields.flatMap { grandchildField ->
              val grandchildFieldPath = "$childFieldPath.${grandchildField.name}"
              persister.getPropertyColumnNames(grandchildFieldPath).map { ColumnName(it) to FieldPath(grandchildFieldPath) }
            }
          }
        }
      }
    }.toMap()[columnName]!!

  private fun FieldPath.getFieldName(): FieldName = this.value.split(".").last().let { FieldName(it) }

  private fun translateValue(updatedValue: String, fieldPath: FieldPath): Any =
    when (getRecallField(fieldPath).type) {
      java.lang.Boolean::class.java -> booleanValue(updatedValue)
      LocalDate::class.java -> LocalDate.parse(updatedValue)
      OffsetDateTime::class.java -> OffsetDateTime.parse(updatedValue.replace(" ", "T"))
      UUID::class.java -> UUID.fromString(updatedValue)
      Int::class.java -> updatedValue.toInt()
      else -> updatedValue
    }
}
