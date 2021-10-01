package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.time.LocalDate

class RevocationOrderHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {

  private val underTest = RevocationOrderGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      RevocationOrderContext(
        ::RecallId.random(),
        FirstAndMiddleNames(FirstName("PrisonerFirstName"), MiddleNames("PrisonerMiddleNames")),
        LastName("PrisonerLastName"),
        LocalDate.of(1999, 12, 31),
        "PrisonerBookNumber",
        "PrisonerCroNumber",
        "01 Sep 2021",
        "30 Sep 2020",
        "assessedByUserDetailsSignature"
      )
    )

    approver.assertApproved(generatedHtml)
  }
}
