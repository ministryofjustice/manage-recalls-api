package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.HtmlGenerationTestCase
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import java.time.LocalDate

class OffenderNotificationHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = OffenderNotificationGenerator(templateEngine)

  @Test
  fun `generate offender notification HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        OffenderNotificationContext(
          FullName("Byron Badger"),
          BookingNumber("A1234"),
          LocalDate.of(2021, 10, 3),
          listOf(ReasonForRecall.ELM_FURTHER_OFFENCE.label, ReasonForRecall.BREACH_EXCLUSION_ZONE.label, ReasonForRecall.FAILED_HOME_VISIT.label).sorted()
        )
      )
    )
  }
}
