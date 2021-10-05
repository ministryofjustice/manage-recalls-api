package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Component
class ReasonsForRecallGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {
  fun generateHtml(reasonsForRecallContext: ReasonsForRecallContext): String =
    Context().apply {
      setVariable("firstNames", reasonsForRecallContext.firstAndMiddleNames)
      setVariable("lastName", reasonsForRecallContext.lastName)
      setVariable("prisonNumber", reasonsForRecallContext.bookingNumber)
      setVariable("pnomisNumber", reasonsForRecallContext.nomsNumber)
      setVariable("licenceConditionsBreached", reasonsForRecallContext.licenceConditionsBreached)
    }.let {
      templateEngine.process("reasons-for-recall", it)
    }
}
