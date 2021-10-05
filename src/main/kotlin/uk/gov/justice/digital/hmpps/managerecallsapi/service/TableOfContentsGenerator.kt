package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine

@Component
class TableOfContentsGenerator(
  @Autowired private val templateEngine: SpringTemplateEngine
) {

  fun generateHtml(
    tableOfContentsContext: TableOfContentsContext,
    tableOfContentsItems: List<TableOfContentsItem>
  ): String =
    Context().apply {
      setVariable("logoFileName", RecallImage.HmppsLogo.fileName)
      setVariable(
        "recallLengthAndSentenceHeading",
        tableOfContentsContext.recallLengthDescription.tableOfContentsFixedTermLengthDescription()
      )
      setVariable("fullName", tableOfContentsContext.fullName)
      setVariable("establishment", tableOfContentsContext.currentPrisonName)
      setVariable("category", "Not Applicable")
      setVariable("prisonNumber", tableOfContentsContext.bookingNumber)
      setVariable("version", "0")
      setVariable("tableOfContentsItems", tableOfContentsItems)
    }.let {
      templateEngine.process("table-of-contents", it)
    }
}
