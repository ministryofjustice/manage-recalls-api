package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall

@Component
class TableOfContentsGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(context: TableOfContentsContext): String =
    Context().apply {
      val fullName = String.format("%s %s %s", context.prisoner.firstName ?: "", context.prisoner.middleNames ?: "", context.prisoner.lastName ?: "").trim()

      setVariable("logoFileName", RecallImage.HmppsLogo.fileName)
      setVariable("recallLengthAndSentenceHeading", recallLengthAndSentenceHeading(context.recall))
      setVariable("fullName", fullName)
      setVariable("establishment", context.currentPrisonName)
      setVariable("category", "Not Applicable")
      setVariable("prisonNumber", context.recall.bookingNumber)
      setVariable("version", "0")
      setVariable("documents", context.documents)
    }.let {
      templateEngine.process("table-of-contents", it)
    }

  private fun recallLengthAndSentenceHeading(recall: Recall) =
    when (recall.recallLength!!) {
      FOURTEEN_DAYS -> "14 Day FTR under 12 months"
      TWENTY_EIGHT_DAYS -> "28 Day FTR 12 months & over"
    }
}
