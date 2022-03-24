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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.LocalDate

class LetterToPrisonGovernorHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val underTest = LetterToPrisonGovernorGenerator(templateEngine)

  @Test
  fun `generate fully populated HTML FTR 14 days`(approver: ContentApprover) {
    val recallLength = RecallLength.FOURTEEN_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        context(RecallType.FIXED, recallLength)
      )
    )
  }

  @Test
  fun `generate fully populated HTML FTR 28 days`(approver: ContentApprover) {
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        context(RecallType.FIXED, recallLength)
      )
    )
  }

  @Test
  fun `generate fully populated HTML Standard recall`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        context(RecallType.STANDARD, null)
      )
    )
  }

  private fun context(recallType: RecallType, recallLength: RecallLength?) =
    LetterToPrisonGovernorContext(
      FullName("Billie Badger"),
      PrisonName("Prison A"),
      PrisonName("Prison B"),
      RecallDescription(recallType, recallLength),
      BookingNumber("B1234"),
      LocalDate.of(2020, 10, 1),
      LocalDate.of(2021, 10, 4),
    )
}
