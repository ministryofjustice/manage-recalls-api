package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate

class GetRecallNotificationComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedPdf = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
  private val expectedBase64Pdf = expectedPdf.encodeToBase64String()

  @Test
  fun `get recall notification returns merged recall summary and revocation order`() {
    val userId = ::UserId.random()
    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    updateRecallWithRequiredInformationForTheRecallNotification(recall.recallId, userId)

    setupUserDetailsFor(userId)
    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)
    gotenbergMockServer.stubGenerateRevocationOrder(expectedPdf, firstName)
    gotenbergMockServer.stubGenerateRecallSummary(expectedPdf)
    gotenbergMockServer.stubGenerateLetterToProbation(expectedPdf, "28 DAY FIXED TERM RECALL")

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
    )

    val response = authenticatedClient.getRecallNotification(recall.recallId, userId)

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }

  private fun updateRecallWithRequiredInformationForTheRecallNotification(recallId: RecallId, userId: UserId) {
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        mappaLevel = MappaLevel.LEVEL_3,
        previousConvictionMainName = "Bryan Badger",
        bookingNumber = "B1234",
        lastReleasePrison = PrisonId("AKI"),
        lastReleaseDate = LocalDate.of(2021, 9, 2),
        sentenceDate = LocalDate.of(2012, 5, 17),
        licenceExpiryDate = LocalDate.of(2020, 11, 1),
        sentenceExpiryDate = LocalDate.of(2020, 10, 29),
        sentenceLength = Api.SentenceLength(2, 3, 10),
        sentencingCourt = CourtId("ACCRYC"),
        indexOffence = "Some offence",
        reasonsForRecall = setOf(POOR_BEHAVIOUR_FURTHER_OFFENCE),
        probationOfficerName = "Mr Probation Officer",
        probationOfficerPhoneNumber = "01234567890",
        probationOfficerEmail = "officer@myprobation.com",
        localDeliveryUnit = LocalDeliveryUnit.PS_BARNET,
        authorisingAssistantChiefOfficer = "Ms Authoriser",
        localPoliceForce = "London",
        currentPrison = PrisonId("BMI"),
        contraband = true,
        contrabandDetail = "Some contraband detail",
        vulnerabilityDiversity = true,
        vulnerabilityDiversityDetail = "Some stuff",
        assessedByUserId = userId,

      )
    )
  }

  private fun expectAPrisonerWillBeFoundFor(nomsNumber: NomsNumber, firstName: String) {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber.value,
          croNumber = "CRO Num/456",
          firstName = firstName,
          lastName = "Badger",
          dateOfBirth = LocalDate.of(2000, 1, 31)
        )
      )
    )
  }
}
