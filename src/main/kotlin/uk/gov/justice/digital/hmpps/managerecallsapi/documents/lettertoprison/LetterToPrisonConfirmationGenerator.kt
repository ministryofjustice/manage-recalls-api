package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.personName

@Component
class LetterToPrisonConfirmationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {
      setVariable("fullName", context.prisoner.personName())

      with(context.recall) {
        val recallLengthDescription = RecallLengthDescription(this.recallLength!!)
        setVariable("recallLengthDescription", recallLengthDescription.asFixedTermLengthDescription())
        setVariable("recallLengthDays", recallLengthDescription.numberOfDays())
        setVariable("bookingNumber", this.bookingNumber)
      }
    }.let {
      templateEngine.process("letter-to-prison_confirmation-of-rar", it)
    }
}
