package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import java.time.LocalDate

class TableOfContentsHtmlGeneratorTest(
  @Autowired private val templateEngine: SpringTemplateEngine
) : HtmlGenerationTestCase() {

  private val underTest = TableOfContentsGenerator(templateEngine)

  @Test
  fun `generate revocation order HTML`(approver: ContentApprover) {
    val generatedHtml = underTest.generateHtml(
      TableOfContentsContext(
        Recall(
          ::RecallId.random(),
          randomNoms(),
          recallLength = TWENTY_EIGHT_DAYS,
          lastReleaseDate = LocalDate.of(2020, 9, 30),
          bookingNumber = "ABC1234F"
        ),
        Prisoner(
          firstName = "PrisonerFirstName",
          middleNames = "PrisonerMiddleNames",
          lastName = "PrisonerLastName"
        ),
        PrisonName("Current Prison (ABC)"),
        listOf(Document("Document 1", 1), Document("Document 2", 3), Document("Document 3", 7))
      )
    )

    approver.assertApproved(generatedHtml)
  }
}
