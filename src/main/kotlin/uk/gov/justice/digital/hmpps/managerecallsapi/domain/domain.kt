package uk.gov.justice.digital.hmpps.managerecallsapi.domain

import java.util.UUID
import javax.validation.constraints.NotBlank

// Now we get the error message:  'nomsNumber.value: nomsNumber must not be blank', which breaks the current Pact contract
data class NomsNumber(@field:NotBlank val value: String)
data class RecallId(val value: UUID)
