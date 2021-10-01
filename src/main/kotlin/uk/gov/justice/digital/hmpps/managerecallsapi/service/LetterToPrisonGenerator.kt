package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Component
class LetterToPrisonGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine,
) {
  fun generateHtml(): String =
    Context().let {
      templateEngine.process("letter-to-probation", it)
    }
}
