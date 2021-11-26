package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber

class ReasonsForRecallHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = ReasonsForRecallGenerator(templateEngine)

  @Test
  fun `generate HTML`(approver: ContentApprover) {
    val html = underTest.generateHtml(
      ReasonsForRecallContext(
        FullName("Bertie Badger"),
        "B1234",
        NomsNumber("G4995VC"),
        "(i) breach one\n(ii) breach two"
      )
    )
    approver.assertApproved(html)
  }
}
