package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ProbationDivision
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate
import java.util.Base64

class GetRecallNotificationComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedPdf = "Expected Generated PDF".toByteArray()
  private val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

  @Test
  fun `get recall notification returns merged recall summary and revocation order`() {
    val recallId = ::RecallId.random()
    val recall = Recall(
      recallId,
      nomsNumber,
      mappaLevel = MappaLevel.LEVEL_3,
      contrabandDetail = "Some contraband detail",
      previousConvictionMainName = "Bryan Badger",
      bookingNumber = "B1234",
      lastReleaseDate = LocalDate.of(2020, 10, 1),
      reasonsForRecall = setOf(
        ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE
      ),
      sentencingInfo = SentencingInfo(
        LocalDate.of(2020, 10, 1),
        LocalDate.of(2020, 11, 1),
        LocalDate.of(2020, 10, 29),
        "High Court",
        "Some offence",
        SentenceLength(2, 3, 10),
      ),
      probationInfo = ProbationInfo(
        "Mr Probation Officer",
        "01234567890",
        "officer@myprobation.com",
        ProbationDivision.LONDON,
        "Ms Authoriser"
      ),
      localPoliceForce = "London",
      vulnerabilityDiversityDetail = "Some stuff",
      currentPrison = "AKI",
      lastReleasePrison = "BMI"
    )
    recallRepository.save(recall)

    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)
    gotenbergMockServer.stubPdfGeneration(expectedPdf, firstName, "revocation-order-logo")
    gotenbergMockServer.stubPdfGeneration(expectedPdf, "OFFENDER IS IN CUSTODY", "recall-summary-logo")
    gotenbergMockServer.stubPdfGeneration(expectedPdf, "licence was revoked today as a Fixed Term Recall", "letter-to-probation-logo")

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      "1-recallSummary.pdf" to expectedPdf.decodeToString(),
      "2-revocationOrder.pdf" to expectedPdf.decodeToString(),
      "3-letterToProbation.pdf" to expectedPdf.decodeToString(),
    )

    val response = authenticatedClient.getRecallNotification(recallId)

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }

  private fun expectAPrisonerWillBeFoundFor(nomsNumber: NomsNumber, firstName: String) {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber.value,
          firstName = firstName,
          lastName = "Oskina",
          dateOfBirth = LocalDate.of(2000, 1, 31),
          bookNumber = "Book Num 123",
          croNumber = "CRO Num/456"
        )
      )
    )
  }
}
