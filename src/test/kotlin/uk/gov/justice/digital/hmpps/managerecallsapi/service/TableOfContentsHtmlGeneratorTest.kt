package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.spring5.SpringTemplateEngine
import uk.gov.justice.digital.hmpps.managerecallsapi.approval.ContentApprover
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
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
          sentencingInfo = SentencingInfo(LocalDate.now(), LocalDate.now(), LocalDate.now(), "", "", SentenceLength(1, 0, 0)),
          lastReleaseDate = LocalDate.of(2020, 9, 30),
          bookingNumber = "ABC1234F"
        ),
        Prisoner(
          firstName = "PrisonerFirstName",
          middleNames = "PrisonerMiddleNames",
          lastName = "PrisonerLastName"
        ),
        "Current Prison (ABC)",
        listOf(Document("Document 1", 1), Document("Document 2", 3), Document("Document 3", 7))
      )
    )

    approver.assertApproved(generatedHtml)
  }
}
