package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner

class LetterToPrisonCustodyOfficeHtmlGenerationTest(@Autowired private val templateEngine: SpringTemplateEngine) :
  HtmlGenerationTestCase() {
  private val underTest = LetterToPrisonCustodyOfficeGenerator(templateEngine)

  @Test
  fun `generate fully populated HTML`(approver: ContentApprover) {
    approver.assertApproved(
      underTest.generateHtml(
        LetterToPrisonContext(
          Recall(::RecallId.random(), NomsNumber("AA1234A"), recallLength = RecallLength.FOURTEEN_DAYS),
          Prisoner(),
          PrisonName("Prison A")
        )
      )
    )
  }
}
