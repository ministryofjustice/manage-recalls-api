package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldPath
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallAuditService
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class AuditController(
  @Autowired private val recallAuditService: RecallAuditService,
) {

  @Operation(summary = "Returns a list of historic values for the given fieldPath of the recall associated with the given recallId")
  @GetMapping("/audit/{recallId}/{fieldPath}")
  fun auditForField(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("fieldPath") fieldPath: FieldPath
  ): List<FieldAuditEntry> =
    recallAuditService.getAuditForField(recallId, fieldPath)

  @Operation(summary = "Returns a summary for each value of the recall associated with the given recallId")
  @GetMapping("/audit/{recallId}")
  fun auditSummaryForRecall(
    @PathVariable("recallId") recallId: RecallId
  ): List<FieldAuditSummary> =
    recallAuditService.getAuditSummaryForRecall(recallId)
}

data class FieldAuditEntry(
  val auditId: Int,
  val recallId: RecallId,
  val updatedByUserName: FullName,
  val updatedDateTime: OffsetDateTime,
  val updatedValue: Any
)

data class FieldAuditSummary(
  val auditId: Int,
  val fieldName: FieldName,
  val fieldPath: FieldPath,
  val updatedByUserName: FullName,
  val updatedDateTime: OffsetDateTime,
  val auditCount: Int
)
