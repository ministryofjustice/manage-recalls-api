package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Component
class ReasonsForRecallGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(context: ReasonsForRecallContext): String =
    Context().apply {
      val firstAndMiddleNames = String.format("%s %s", context.prisoner.firstName ?: "", context.prisoner.middleNames ?: "").trim()
      setVariable("firstNames", firstAndMiddleNames)
      setVariable("lastName", context.prisoner.lastName)
      setVariable("prisonNumber", context.recall.bookingNumber)
      setVariable("pnomisNumber", context.recall.nomsNumber)
      setVariable("licenceConditionsBreached", context.recall.licenceConditionsBreached)
    }.let {
      templateEngine.process("reasons-for-recall", it)
    }
}
