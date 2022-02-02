package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import java.time.LocalDate

class LetterToProbationHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = LetterToProbationGenerator(templateEngine)

  @Test
  fun `generate in custody letter to probation HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        letterToProbationContext(true),
      )
    )
  }

  @Test
  fun `generate not in custody letter to probation HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        letterToProbationContext(false),
      )
    )
  }

  private fun letterToProbationContext(inCustody: Boolean) = LetterToProbationContext(
    LocalDate.of(2021, 9, 29),
    RecallLengthDescription(FOURTEEN_DAYS),
    "Mr probation",
    FullName("Bertie Offender"),
    "bookingNumber",
    PrisonName("Current prison name"),
    PersonName("Bobby", lastName = "Caseworker"),
    inCustody
  )
}
