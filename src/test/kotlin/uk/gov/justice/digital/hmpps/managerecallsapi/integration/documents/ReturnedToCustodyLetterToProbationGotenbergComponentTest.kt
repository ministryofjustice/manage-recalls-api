package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import java.time.LocalDate
import java.time.OffsetDateTime

class ReturnedToCustodyLetterToProbationGotenbergComponentTest : GotenbergComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val assessedByUserId = ::UserId.random()

  @Test
  fun `can generate a returned to custody fixed term recall letter to probation using gotenberg`() {
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
    updateRecallWithRequiredInformationForNotInCustodyLetterToProbation(
      recall.recallId,
      recallType = RecallType.FIXED
    )

    val letterToProbationId = authenticatedClient.generateDocument(recall.recallId, LETTER_TO_PROBATION, FileName("BILLIE BOBS LETTER TO PROBATION.pdf"))
    val letterToProbation = authenticatedClient.getDocument(recall.recallId, letterToProbationId.documentId)
    // writeBase64EncodedStringToFile("returned-to-custody-28-day-FTR-letter-to-probation.pdf", letterToProbation.content)
    assertThat(Pdf(letterToProbation.content), hasNumberOfPages(equalTo(1)))
  }

  @Test
  fun `can generate a returned to custody standard recall letter to probation using gotenberg`() {
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
    updateRecallWithRequiredInformationForNotInCustodyLetterToProbation(
      recall.recallId,
      recallType = RecallType.STANDARD
    )

    val letterToProbationId = authenticatedClient.generateDocument(recall.recallId, LETTER_TO_PROBATION, FileName("BILLIE BOBS LETTER TO PROBATION.pdf"))
    val letterToProbation = authenticatedClient.getDocument(recall.recallId, letterToProbationId.documentId)
    // writeBase64EncodedStringToFile("returned-to-custody-standard-letter-to-probation.pdf", letterToProbation.content)
    assertThat(Pdf(letterToProbation.content), hasNumberOfPages(equalTo(2)))
  }

  private fun updateRecallWithRequiredInformationForNotInCustodyLetterToProbation(
    recallId: RecallId,
    recallType: RecallType
  ) {
    authenticatedClient.updateRecommendedRecallType(recallId, recallType)
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        licenceNameCategory = NameFormatCategory.FIRST_LAST,
        mappaLevel = MappaLevel.LEVEL_1,
        bookingNumber = "NAT0001",
        sentenceDate = LocalDate.of(2012, 5, 17),
        licenceExpiryDate = LocalDate.of(2025, 12, 25),
        sentenceExpiryDate = LocalDate.of(2021, 1, 12),
        sentenceLength = Api.SentenceLength(2, 1, 5),
        sentencingCourt = CourtId("HVRFCT"),
        indexOffence = "Some index offence",
        probationOfficerName = "Percy Pig",
        probationOfficerPhoneNumber = "0898909090",
        probationOfficerEmail = "probation.officer@moj.com",
        localDeliveryUnit = LocalDeliveryUnit.ISLE_OF_MAN,
        authorisingAssistantChiefOfficer = "Mr ACO",
        currentPrison = PrisonId("MWI"),
        differentNomsNumber = true,
        differentNomsNumberDetail = "ABC123",
        inCustodyAtBooking = false,
        inCustodyAtAssessment = false,
      )
    )
    authenticatedClient.returnedToCustody(recallId, OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(1))
  }
}
