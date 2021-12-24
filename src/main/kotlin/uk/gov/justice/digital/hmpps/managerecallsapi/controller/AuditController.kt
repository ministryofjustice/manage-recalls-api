package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FieldName
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

  @GetMapping("/audit/{recallId}/{fieldName}")
  fun auditForField(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("fieldName") fieldName: FieldName
  ): List<FieldAuditEntry> =
    recallAuditService.getAuditForFieldName(recallId, fieldName)
}

data class FieldAuditEntry(
  val auditId: Int,
  val recallId: RecallId,
  val updatedByUserName: FullName,
  val updatedDateTime: OffsetDateTime,
  val updatedValue: Any
)
