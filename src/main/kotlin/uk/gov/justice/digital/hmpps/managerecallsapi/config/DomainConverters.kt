package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.util.UUID

// Used to convert API URI PathVariables from String to type.
@Component
class RecallIdConverter : CustomConverter<String, RecallId>({ RecallId(UUID.fromString(it)) })
@Component
class UserIdConverter : CustomConverter<String, UserId>({ UserId(UUID.fromString(it)) })
@Component
class DocumentIdConverter : CustomConverter<String, DocumentId>({ DocumentId(UUID.fromString(it)) })
@Component
class LastKnownAddressIdConverter : CustomConverter<String, LastKnownAddressId>({ LastKnownAddressId(UUID.fromString(it)) })
@Component
class RescindRecordIdConverter : CustomConverter<String, RescindRecordId>({ RescindRecordId(UUID.fromString(it)) })

abstract class CustomConverter<IN, OUT>(private val toTypeFn: (IN) -> OUT) : Converter<IN, OUT> {
  override fun convert(value: IN): OUT = toTypeFn(value)
}
