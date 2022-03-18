package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName

class LetterToPrisonConfirmationHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val underTest = LetterToPrisonConfirmationGenerator(templateEngine)

  @Test
  fun `generate fully populated HTML`(approver: ContentApprover) {
    val recallLength = RecallLength.FOURTEEN_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonConfirmationContext(
          FullName("Billie Badger"),
          RecallDescription(RecallType.FIXED, recallLength),
          BookingNumber("B1234"),
        )
      )
    )
  }
}
