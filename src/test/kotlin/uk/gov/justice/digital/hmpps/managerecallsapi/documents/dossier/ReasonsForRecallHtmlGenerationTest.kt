package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.FirstAndMiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.service.ReasonsForRecallContext

class ReasonsForRecallHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = ReasonsForRecallGenerator(templateEngine)

  @Test
  fun `generate HTML`(approver: ContentApprover) {
    val html = underTest.generateHtml(
      ReasonsForRecallContext(
        FirstAndMiddleNames(FirstName("Bertie"), MiddleNames("Basset")),
        LastName("Badger"),
        "B1234",
        NomsNumber("G4995VC"),
        "(i) breach one\n(ii) breach two"
      )
    )
    approver.assertApproved(html)
  }
}
