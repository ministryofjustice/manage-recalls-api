package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Component
class LetterToProbationGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(letterToProbationContext: LetterToProbationContext): String =
    Context().apply {
      setVariable("logoFileName", HmppsLogo.fileName)
    }.let {
      templateEngine.process("letter-to-probation", it)
    }
}
