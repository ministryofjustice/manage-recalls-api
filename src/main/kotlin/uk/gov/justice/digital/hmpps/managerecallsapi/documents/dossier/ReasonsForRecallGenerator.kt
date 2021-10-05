package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReasonsForRecallContext

@Component
class ReasonsForRecallGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {
  fun generateHtml(reasonsForRecallContext: ReasonsForRecallContext): String =
    Context().apply {
      setVariable("firstAndMiddleNames", reasonsForRecallContext.firstAndMiddleNames)
      setVariable("lastName", reasonsForRecallContext.lastName)
      setVariable("bookingNumber", reasonsForRecallContext.bookingNumber)
      setVariable("nomsNumber", reasonsForRecallContext.nomsNumber)
      setVariable("licenceConditionsBreached", reasonsForRecallContext.licenceConditionsBreached)
    }.let {
      templateEngine.process("reasons-for-recall", it)
    }
}
