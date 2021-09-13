package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ApprovalTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ThymeleafConfig
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [ThymeleafConfig::class])
class RevocationOrderHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : ApprovalTestCase() {

  private val underTest = RevocationOrderGenerator(templateEngine)

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
      )
    )

    approver.assertApproved(generatedHtml)
  }

}
