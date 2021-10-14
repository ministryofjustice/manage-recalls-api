package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName

class TableOfContentsHtmlGeneratorTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {

  private val underTest = TableOfContentsGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      TableOfContentsContext(
        PersonName("PrisonerFirstName", "PrisonerLastName"),
        RecallLengthDescription(TWENTY_EIGHT_DAYS),
        PrisonName("Current Prison (ABC)"),
        "ABC1234F"
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
