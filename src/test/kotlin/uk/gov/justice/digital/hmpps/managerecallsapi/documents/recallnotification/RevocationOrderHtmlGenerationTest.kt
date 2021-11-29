package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate

class RevocationOrderHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {

  private val underTest = RevocationOrderGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      RevocationOrderContext(
        ::RecallId.random(),
        FullName("PrisonerFirstName PrisonerLastName"),
        LocalDate.of(1999, 12, 31),
        "RecallBookingNumber",
        "PrisonerCroNumber",
        LocalDate.of(2021, 9, 1),
        LocalDate.of(2020, 9, 30),
        "assessedByUserDetailsSignature"
      )
    )

    approver.assertApproved(generatedHtml)
  }
}
