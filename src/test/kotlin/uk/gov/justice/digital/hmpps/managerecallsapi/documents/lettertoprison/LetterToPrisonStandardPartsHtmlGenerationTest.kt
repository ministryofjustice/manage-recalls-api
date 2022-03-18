package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.LocalDate

class LetterToPrisonStandardPartsHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val underTest = LetterToPrisonStandardPartsGenerator(templateEngine)

  @Test
  fun `generate fully populated Part 1 HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generatePart1Html(
        LetterToPrisonStandardPartsContext(
          FullName("Billie Badger"),
          BookingNumber("B1234"),
          LocalDate.now(),
          PrisonName("HMP Prison")
        )
      )
    )
  }

  @Test
  fun `generate fully populated Part 2 HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generatePart2Html(
        LetterToPrisonStandardPartsContext(
          FullName("Billie Badger"),
          BookingNumber("B1234"),
          LocalDate.of(2022, 3, 17),
          PrisonName("HMP Prison")
        )
      )
    )
  }

  @Test
  fun `generate fully populated Part 3 HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generatePart3Html(
        LetterToPrisonStandardPartsContext(
          FullName("Billie Badger"),
          BookingNumber("B1234"),
          LocalDate.now(),
          PrisonName("HMP Prison")
        )
      )
    )
  }
}
