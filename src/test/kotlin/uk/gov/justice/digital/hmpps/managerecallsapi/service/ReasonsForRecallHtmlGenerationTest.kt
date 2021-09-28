package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner

class ReasonsForRecallHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = ReasonsForRecallGenerator(templateEngine)

  @Test
  fun `generate HTML`(approver: ContentApprover) {
    val nomsNumber = NomsNumber("G4995VC")
    val html = underTest.generateHtml(
      ReasonsForRecallContext(
        Recall(
          ::RecallId.random(),
          nomsNumber,
          bookingNumber = "B1234",
          licenceConditionsBreached = "(i) breach one\n(ii) breach two"
        ),
        Prisoner(
          firstName = "Bertie",
          middleNames = "Basset",
          lastName = "Badger",
          bookNumber = "bookNumber"
        )
      )
    )
    approver.assertApproved(html)
  }
}
