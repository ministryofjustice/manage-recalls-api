package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName

class TableOfContentsHtmlGeneratorTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {

  private val underTest = TableOfContentsGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML for Fixed recall`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      TableOfContentsContext(
        FullName("PrisonerFirstName PrisonerLastName"),
        RecallDescription(RecallType.FIXED, TWENTY_EIGHT_DAYS),
        PrisonName("Current Prison (ABC)"),
        "ABC1234F",
        2
      ),
      listOf(
        TableOfContentsItem("Document 1", 1),
        TableOfContentsItem("Document 2", 3),
        TableOfContentsItem("Document 3", 7)
      )
    )

    approver.assertApproved(generatedHtml)
  }

  @Test
  fun `generate revocation order HTML for standard recall`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      TableOfContentsContext(
        FullName("PrisonerFirstName PrisonerLastName"),
        RecallDescription(RecallType.STANDARD, null),
        PrisonName("Current Prison (ABC)"),
        "ABC1234F",
        2
      ),
      listOf(
        TableOfContentsItem("Document 1", 1),
        TableOfContentsItem("Document 2", 3),
        TableOfContentsItem("Document 3", 7)
      )
    )

    approver.assertApproved(generatedHtml)
  }
}
