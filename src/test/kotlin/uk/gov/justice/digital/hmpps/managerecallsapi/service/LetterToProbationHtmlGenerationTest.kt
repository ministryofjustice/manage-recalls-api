package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import java.time.LocalDate

class LetterToProbationHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = LetterToProbationGenerator(templateEngine)

  @Test
  fun `generate HTML`(approver: ContentApprover) {
    val letterToProbationContext = LetterToProbationContext(
      LocalDate.of(2021, 9, 29),
      RecallLengthDescription(FOURTEEN_DAYS),
      "Mr probation",
      FirstAndLastName(FirstName("Bertie"), LastName("Offender")),
      "bookingNumber",
      "Current prison name",
      FirstAndLastName(FirstName("Bobby"), LastName("Caseworker"))
    )
    approver.assertApproved(underTest.generateHtml(letterToProbationContext))
  }
}
