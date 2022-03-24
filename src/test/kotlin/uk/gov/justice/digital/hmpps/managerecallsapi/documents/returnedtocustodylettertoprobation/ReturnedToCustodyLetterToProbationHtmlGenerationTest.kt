package uk.gov.justice.digital.hmpps.managerecallsapi.documents.returnedtocustodylettertoprobation

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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.LocalDate

class ReturnedToCustodyLetterToProbationHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val underTest = ReturnedToCustodyLetterToProbationGenerator(templateEngine)
  private val originalCreatedDate = LocalDate.of(2022, 3, 16)

  @Test
  fun `generate fully populated HTML for fixed term recall`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        ReturnedToCustodyLetterToProbationContext(
          RecallDescription(RecallType.FIXED, RecallLength.FOURTEEN_DAYS),
          BookingNumber("B12345C"),
          NomsNumber("AB1234C"),
          true,
          NomsNumber("ZY9876X"),
          "Probation Paul",
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          originalCreatedDate.minusDays(1),
          originalCreatedDate,
          "Mr ACO",
          null,
        )
      )
    )
  }

  @Test
  fun `generate fully populated HTML for standard recall`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        ReturnedToCustodyLetterToProbationContext(
          RecallDescription(RecallType.STANDARD, null),
          BookingNumber("B12345C"),
          NomsNumber("AB1234C"),
          true,
          NomsNumber("ZY9876X"),
          "Probation Paul",
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          originalCreatedDate.minusDays(1),
          originalCreatedDate,
          "Mr ACO",
          originalCreatedDate.plusDays(10),
        )
      )
    )
  }
}
