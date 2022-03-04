package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
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
  fun `generate in custody fixed recall letter to probation HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        letterToProbationContext(true, FIXED),
      )
    )
  }

  @Test
  fun `generate not in custody fixed recall letter to probation HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        letterToProbationContext(false, FIXED),
      )
    )
  }

  @Test
  fun `generate in custody standard recall letter to probation HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        letterToProbationContext(true, STANDARD),
      )
    )
  }

  @Test
  fun `generate not in custody standard recall letter to probation HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        letterToProbationContext(false, STANDARD),
      )
    )
  }

  private fun letterToProbationContext(inCustody: Boolean, recallType: RecallType) = LetterToProbationContext(
    LocalDate.of(2021, 9, 29),
    RecallLengthDescription(FOURTEEN_DAYS),
    "Mr probation",
    FullName("Bertie Offender"),
    "bookingNumber",
    if (!inCustody && recallType == FIXED) null else PrisonName("Current prison name"),
    PersonName("Bobby", lastName = "Caseworker"),
    inCustody,
    recallType
  )
}
