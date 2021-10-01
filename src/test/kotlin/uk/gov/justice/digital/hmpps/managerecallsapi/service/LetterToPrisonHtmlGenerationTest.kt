package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover

class LetterToPrisonHtmlGenerationTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {
  private val underTest = LetterToPrisonGenerator(templateEngine)

  @Test
  fun `generate HTML`(approver: ContentApprover) {
    val html = underTest.generateHtml()
    approver.assertApproved(html)
  }
}
