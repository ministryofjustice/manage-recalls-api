package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames

@Component
class LetterToPrisonConfirmationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {

      with(context.prisoner) {
        setVariable("fullName", PersonName(FirstName(this.firstName!!), this.middleNames?.let { MiddleNames(it) }, LastName(this.lastName!!)))
      }

      with(context.recall) {
        setVariable("recallLengthDescription", RecallLengthDescription(this.recallLength!!).asFixedTermLengthDescription())
        setVariable("bookingNumber", this.bookingNumber)
      }
    }.let {
      templateEngine.process("letter-to-prison_confirmation-of-rar", it)
    }
}
