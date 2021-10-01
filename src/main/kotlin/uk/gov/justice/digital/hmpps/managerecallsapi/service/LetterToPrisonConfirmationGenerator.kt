package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.time.LocalDate

@Component
class LetterToPrisonConfirmationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generateHtml(context: LetterToPrisonContext): String =
    Context().apply {

      setVariable("logoFileName", RecallImage.HmppsLogo.fileName)
      setVariable("teamName", RECALL_TEAM_NAME)
      setVariable("teamPhoneNumber", RECALL_TEAM_CONTACT_NUMBER)
      setVariable("todaysDate", LocalDate.now().asStandardDateFormat())
      with(context.recall) {
        setVariable("recallLength", RecallLengthDescription(this.recallLength!!).asFixedTermLengthDescription())
      }
      setVariable("currentEstablishment", context.currentPrisonName)

//      setVariable("licenceRevocationDate", context.licenceRevocationDate.asStandardDateFormat())
    }.let {
      templateEngine.process("letter-to-prison_confirmation-of-rar", it)
    }
}
