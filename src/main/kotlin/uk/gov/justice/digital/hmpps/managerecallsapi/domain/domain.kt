package uk.gov.justice.digital.hmpps.managerecallsapi.domain

import java.util.UUID
import javax.validation.constraints.NotBlank

data class NomsNumber(@field:NotBlank val value: String)
data class RecallId(val value: UUID)
