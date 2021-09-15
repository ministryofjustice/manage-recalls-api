package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RevocationOrderHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {

  private val fixedClock = Clock.fixed(Instant.parse("2021-09-01T16:48:30.00Z"), ZoneId.of("UTC"))
  private val underTest = RevocationOrderGenerator(templateEngine, fixedClock)

  @Test
  fun `generate revocation order HTML`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      Prisoner(
        firstName = "PrisonerFirstName",
        middleNames = "PrisonerMiddleNames",
        lastName = "PrisonerLastName",
        dateOfBirth = LocalDate.of(1999, 12, 31),
        bookNumber = "PrisonerBookNumber",
        croNumber = "PrisonerCroNumber"
      ),
      Recall(
        ::RecallId.random(),
        randomNoms(),
        lastReleaseDate = LocalDate.of(2020, 9, 30)
      )
    )

    approver.assertApproved(generatedHtml)
  }

  // TODO: MD  How should we handle any missing required Prisoner or Recall details?
  @Test
  fun `generate revocation order HTML with missing values`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      Prisoner(),
      Recall(::RecallId.random(), randomNoms())
    )

    approver.assertApproved(generatedHtml)
  }
}
