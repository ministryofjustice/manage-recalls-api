package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage

@Component
class LetterToPrisonGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generateHtml(): String =
    Context().apply {

      setVariable("logoFileName", RecallImage.HmppsLogo.fileName)

//      setVariable("licenceRevocationDate", context.licenceRevocationDate.asStandardDateFormat())
    }.let {
      templateEngine.process("letter-to-prison", it)
    }
}
