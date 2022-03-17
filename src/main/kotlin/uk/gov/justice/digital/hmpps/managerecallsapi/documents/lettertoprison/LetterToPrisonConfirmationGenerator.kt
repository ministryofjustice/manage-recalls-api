package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Component
class LetterToPrisonConfirmationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generateHtml(context: LetterToPrisonConfirmationContext): String =
    Context().apply {
      setVariable("prisonerNameOnLicence", context.prisonerNameOnLicence)

      setVariable("recallTitle", context.recallDescription.asTitle())
      setVariable("recallLengthDays", context.recallDescription.numberOfDays())
      setVariable("bookingNumber", context.bookingNumber)
    }.let {
      templateEngine.process("letter-to-prison_fixed-term_confirmation-of-rar", it)
    }
}
