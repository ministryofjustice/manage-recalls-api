package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName

class TableOfContentsHtmlGeneratorTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {

  private val underTest = TableOfContentsGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      TableOfContentsContext(
        PersonName(FirstName("PrisonerFirstName"), MiddleNames("PrisonerMiddleNames"), LastName("PrisonerLastName")),
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
