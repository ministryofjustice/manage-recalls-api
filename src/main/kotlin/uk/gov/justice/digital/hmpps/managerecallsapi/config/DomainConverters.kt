package uk.gov.justice.digital.hmpps.managerecallsapi.config

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.util.UUID

@Component
class RecallIdConverter : CustomConverter<String, RecallId>({ RecallId(UUID.fromString(it)) })

abstract class CustomConverter<IN, OUT>(private val toTypeFn: (IN) -> OUT) : Converter<IN, OUT> {
  override fun convert(value: IN): OUT = toTypeFn(value)
}
