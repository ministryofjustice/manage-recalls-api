package uk.gov.justice.digital.hmpps.managerecallsapi.db

import java.util.UUID

data class RecallReason(
  val recallId: UUID,
  val reasonForRecall: ReasonForRecall
)
