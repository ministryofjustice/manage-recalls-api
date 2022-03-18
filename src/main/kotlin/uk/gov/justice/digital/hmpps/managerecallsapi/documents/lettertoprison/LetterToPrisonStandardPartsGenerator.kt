package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.asStandardDateFormat

@Component
class LetterToPrisonStandardPartsGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generatePart1Html(context: LetterToPrisonStandardPartsContext): String =
    Context().apply {
      setVariable("prisonerNameOnLicence", context.prisonerNameOnLicence)
      setVariable("bookingNumber", context.bookingNumber)
    }.let {
      templateEngine.process("letter-to-prison_standard_part-1", it)
    }

  fun generatePart2Html(context: LetterToPrisonStandardPartsContext): String =
    Context().apply {
      setVariable("todaysDate", context.originalCreatedDate.asStandardDateFormat())
      setVariable("currentPrisonName", context.currentPrisonName)
      setVariable("prisonerNameOnLicence", context.prisonerNameOnLicence)
      setVariable("bookingNumber", context.bookingNumber)
    }.let {
      templateEngine.process("letter-to-prison_standard_part-2", it)
    }

  fun generatePart3Html(context: LetterToPrisonStandardPartsContext): String =
    Context().apply {
      setVariable("prisonerNameOnLicence", context.prisonerNameOnLicence)
      setVariable("bookingNumber", context.bookingNumber)
    }.let {
      templateEngine.process("letter-to-prison_standard_part-3", it)
    }
}
