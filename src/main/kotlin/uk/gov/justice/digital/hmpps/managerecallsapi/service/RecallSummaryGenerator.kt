package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Component
class RecallSummaryGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(): String =
    templateEngine.process("recall-summary", Context())
}