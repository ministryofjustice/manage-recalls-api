package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo

@Component
class TableOfContentsGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(
    tableOfContentsContext: TableOfContentsContext,
    tableOfContentsItems: List<TableOfContentsItem>
  ): String =
    Context().apply {
      setVariable("logoFileName", HmppsLogo.fileName)
      setVariable(
        "recallLengthAndSentenceHeading",
        tableOfContentsContext.recallDescription.tableOfContentsDescription()
      )
      setVariable("fullName", tableOfContentsContext.prisonerNameOnLicense)
      setVariable("currentPrisonName", tableOfContentsContext.currentPrisonName)
      setVariable("bookingNumber", tableOfContentsContext.bookingNumber)
      setVariable("tableOfContentsItems", tableOfContentsItems)
      setVariable("category", "Not Applicable")
      setVariable("version", "0 (${tableOfContentsContext.newVersion})")
    }.let {
      templateEngine.process("table-of-contents", it)
    }
}
