package uk.gov.justice.digital.hmpps.managerecallsapi.domain

import java.io.Serializable
import java.util.UUID
import javax.validation.constraints.NotBlank

/*
  If the type will be used json it will need custom Jackson serializer/deserializer:   RestConfiguration
  If it is to be used as a PathVariable it will need a customer Converter:  DomainConverters
 */

// Now we get the error message:  'nomsNumber.value: nomsNumber must not be blank', which breaks the current Pact contract
data class NomsNumber(@field:NotBlank val value: String) {
  override fun toString() = value
}
data class RecallId(val value: UUID) : Serializable {
  override fun toString() = value.toString()
}

fun ((UUID) -> RecallId).random() = this(UUID.randomUUID())
