package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
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
    val userId = ::UserId.random()
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
        LocalDeliveryUnit.PS_BARNET,
        "Ms Authoriser"
      ),
      localPoliceForce = "London",
      vulnerabilityDiversityDetail = "Some stuff",
      currentPrison = "AKI",
      lastReleasePrison = "BMI"
    )
    recallRepository.save(recall)

    userDetailsRepository.save(
      UserDetails(
        userId, FirstName("Bertie"), LastName("Badger"),
        base64EncodedFileContents("/signature.jpg")
      )
    )

    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)
    gotenbergMockServer.stubGenerateRevocationOrder(expectedPdf, firstName)
    gotenbergMockServer.stubPdfGenerationWithHmppsLogo(expectedPdf, "OFFENDER IS IN CUSTODY")
    gotenbergMockServer.stubPdfGenerationWithHmppsLogo(expectedPdf, "licence was revoked today as a Fixed Term Recall")

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
    )

    val response = authenticatedClient.getRecallNotification(recallId, userId)

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
