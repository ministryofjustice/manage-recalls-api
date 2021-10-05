package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Component
class LetterToPrisonConfirmationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {
      setVariable("fullName", context.prisoner.fullName())

      with(context.recall) {
        setVariable("recallLengthDescription", RecallLengthDescription(this.recallLength!!).asFixedTermLengthDescription())
        setVariable("bookingNumber", this.bookingNumber)
      }
    }.let {
      templateEngine.process("letter-to-prison_confirmation-of-rar", it)
    }
}
