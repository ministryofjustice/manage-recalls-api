package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.LocalDate

class LetterToPrisonCustodyOfficeHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val underTest = LetterToPrisonCustodyOfficeGenerator(templateEngine)

  @Test
  fun `generate fully populated HTML FTR 14 days`(approver: ContentApprover) {
    val recallLength = RecallLength.FOURTEEN_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonCustodyOfficeContext(
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          RecallDescription(RecallType.FIXED, recallLength),
          BookingNumber("B1234"),
          FullName("Mandy Pandy"),
          LocalDate.of(2021, 10, 4),
          NomsNumber("ABC1234F"),
          true,
          NomsNumber("AA1234A"),
          true,
          "Blah blah blah",
          true,
          "Because...",
          true,
          "Yes, yadda yadda",
          MappaLevel.LEVEL_2,
        )
      )
    )
  }

  @Test
  fun `generate fully populated HTML FTR 28 days`(approver: ContentApprover) {
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonCustodyOfficeContext(
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          RecallDescription(RecallType.FIXED, recallLength),
          BookingNumber("B1234"),
          FullName("Mandy Pandy"),
          LocalDate.of(2021, 10, 4),
          NomsNumber("ABC1234F"),
          true,
          NomsNumber("AA1234A"),
          true,
          "Blah blah blah",
          true,
          "Because...",
          true,
          "Yes, yadda yadda",
          MappaLevel.LEVEL_2,
        )
      )
    )
  }

  @Test
  fun `generate html when no additional license conditions FTR 14 days`(approver: ContentApprover) {
    val recallLength = RecallLength.FOURTEEN_DAYS
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonCustodyOfficeContext(
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          RecallDescription(RecallType.FIXED, recallLength),
          BookingNumber("B1234"),
          FullName("Mandy Pandy"),
          LocalDate.of(2021, 10, 4),
          NomsNumber("ABC1234F"),
          true,
          NomsNumber("AA1234A"),
          false,
          null,
          true,
          "Because...",
          true,
          "Yes, yadda yadda",
          MappaLevel.LEVEL_2,
        )
      )
    )
  }

  @Test
  fun `generate fully populated HTML Standard recall`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonCustodyOfficeContext(
          FullName("Billie Badger"),
          PrisonName("Prison A"),
          RecallDescription(RecallType.STANDARD, null),
          BookingNumber("B1234"),
          FullName("Mandy Pandy"),
          LocalDate.of(2021, 10, 4),
          NomsNumber("ABC1234F"),
          true,
          NomsNumber("AA1234A"),
          true,
          "Blah blah blah",
          true,
          "Because...",
          true,
          "Yes, yadda yadda",
          MappaLevel.LEVEL_2,
        )
      )
    )
  }
}
