package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import java.time.LocalDate

class LetterToPrisonGotenbergComponentTest : GotenbergComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val assessedByUserId = ::UserId.random()

  @Test
  fun `can generate a letter to prison for a fixed term recall using gotenberg`() {
    setupUserDetailsFor(assessedByUserId)

    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    updateRecallWithRequiredInformationForTheLetterToPrison(
      recall.recallId,
      contrabandDetail = "Contraband 1\nContraband 2",
      vulnerabilityDiversityDetail = "Diversity 1\nDiversity 2",
      assessedByUserId = assessedByUserId,
      sentenceYears = 10,
      recallType = RecallType.FIXED
    )

    val letterToPrisonId = authenticatedClient.generateDocument(recall.recallId, LETTER_TO_PRISON, FileName("LETTER_TO_PRISON.pdf"))
    val letterToPrison = authenticatedClient.getDocument(recall.recallId, letterToPrisonId.documentId)
    // writeBase64EncodedStringToFile("letter-to-prison-14-day-FTR-example.pdf", letterToPrison.content)  // i.e. sentence duration < 1 year
    // writeBase64EncodedStringToFile("letter-to-prison-28-day-FTR-example.pdf", letterToPrison.content)  // i.e. sentence duration >= 1 year

    assertThat(Pdf(letterToPrison.content), hasNumberOfPages(equalTo(5)))
  }

  @Test
  fun `can generate a letter to prison for a standard recall using gotenberg`() {
    setupUserDetailsFor(assessedByUserId)

    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    updateRecallWithRequiredInformationForTheLetterToPrison(
      recall.recallId,
      contrabandDetail = "Contraband 1\nContraband 2",
      vulnerabilityDiversityDetail = "Diversity 1\nDiversity 2",
      assessedByUserId = assessedByUserId,
      sentenceYears = 10,
      recallType = RecallType.STANDARD
    )

    val letterToPrisonId = authenticatedClient.generateDocument(recall.recallId, LETTER_TO_PRISON, FileName("LETTER_TO_PRISON.pdf"))
    val letterToPrison = authenticatedClient.getDocument(recall.recallId, letterToPrisonId.documentId)
    // writeBase64EncodedStringToFile("letter-to-prison-Standard-example.pdf", letterToPrison.content)

    assertThat(Pdf(letterToPrison.content), hasNumberOfPages(equalTo(7)))
  }
}
