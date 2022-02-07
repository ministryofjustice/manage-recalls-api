package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.CreateLastKnownAddressRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LastKnownAddressOption
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.POOR_BEHAVIOUR_FURTHER_OFFENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.LocalDate

class GetRecallNotificationComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedPdf = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
  private val expectedBase64Pdf = expectedPdf.encodeToBase64String()

  @Test
  fun `get recall notification returns merged recall summary and revocation order and then generate recall notification generates new version but reuses existing revocation order`() {
    val userId = authenticatedClient.userId
    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName(firstName),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    updateRecallWithRequiredInformationForTheRecallNotification(recall.recallId, userId, true)
    authenticatedClient.addLastKnownAddress(
      recall.recallId, CreateLastKnownAddressRequest(null, "1 The Road", null, "A Town", "AB12 3CD", AddressSource.MANUAL),
      HttpStatus.CREATED, LastKnownAddressId::class.java
    )

    gotenbergMockServer.stubGenerateRevocationOrder(expectedPdf, firstName)
    gotenbergMockServer.stubGenerateRecallSummary(expectedPdf, "OFFENDER IS IN CUSTODY")
    gotenbergMockServer.stubGenerateLetterToProbation(expectedPdf, "28 DAY FIXED TERM RECALL")

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
    )

    val recallDocId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION)
    val recallDoc = authenticatedClient.getDocument(recall.recallId, recallDocId.documentId)

    assertThat(recallDoc.content, equalTo(expectedBase64Pdf))

    val firstRecallNotification =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, RECALL_NOTIFICATION)!!
    assertThat(firstRecallNotification.version, equalTo(1))
    val firstRevocationOrder =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, REVOCATION_ORDER)!!
    assertThat(firstRevocationOrder.version, equalTo(1))

    // Reset mocks to reassure that revocation order isnt regenerated
    gotenbergMockServer.resetAll()
    gotenbergMockServer.stubGenerateRecallSummary(expectedPdf, "OFFENDER IS IN CUSTODY")
    gotenbergMockServer.stubGenerateLetterToProbation(expectedPdf, "28 DAY FIXED TERM RECALL")

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
    )

    authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION, "Some detail")

    val latestRecallNotification =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, RECALL_NOTIFICATION)!!
    assertThat(latestRecallNotification.version, equalTo(2))
    val latestRevocationOrder =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, REVOCATION_ORDER)!!
    assertThat(latestRevocationOrder.version, equalTo(1))
  }

  @Test
  fun `get recall notification for not in custody includes offender notification and police notification`() {
    val userId = authenticatedClient.userId
    val recall = authenticatedClient.bookRecall(
      BookRecallRequest(
        nomsNumber,
        FirstName(firstName),
        null,
        LastName("Badger"),
        CroNumber("1234/56A"),
        LocalDate.now()
      )
    )
    updateRecallWithRequiredInformationForTheRecallNotification(recall.recallId, userId, false)

    gotenbergMockServer.stubGenerateRevocationOrder(expectedPdf, firstName)
    gotenbergMockServer.stubGenerateRecallSummary(expectedPdf, "RECALL NOTIFICATION")
    gotenbergMockServer.stubGenerateLetterToProbation(expectedPdf, "IF AT ANY TIME THE PROBATION SERVICE RECEIVES INFORMATION THAT MAY AFFECT THE VALIDITY OF THE RECALL ACTION")
    gotenbergMockServer.stubGenerateOffenderNotification(expectedPdf)

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
    )

    val recallDocId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION)
    val recallDoc = authenticatedClient.getDocument(recall.recallId, recallDocId.documentId)

    assertThat(recallDoc.content, equalTo(expectedBase64Pdf))

    val firstRecallNotification =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, RECALL_NOTIFICATION)!!
    assertThat(firstRecallNotification.version, equalTo(1))
    val firstRevocationOrder =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, REVOCATION_ORDER)!!
    assertThat(firstRevocationOrder.version, equalTo(1))

    // Reset mocks to reassure that revocation order is not regenerated
    gotenbergMockServer.resetAll()
    gotenbergMockServer.stubGenerateRecallSummary(expectedPdf, "RECALL NOTIFICATION")
    gotenbergMockServer.stubGenerateLetterToProbation(expectedPdf, "IF AT ANY TIME THE PROBATION SERVICE RECEIVES INFORMATION THAT MAY AFFECT THE VALIDITY OF THE RECALL ACTION")
    gotenbergMockServer.stubGenerateOffenderNotification(expectedPdf)

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
    )

    authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION, "Some detail")

    val latestRecallNotification =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, RECALL_NOTIFICATION)!!
    assertThat(latestRecallNotification.version, equalTo(2))
    val latestRevocationOrder =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, REVOCATION_ORDER)!!
    assertThat(latestRevocationOrder.version, equalTo(1))
  }

  private fun updateRecallWithRequiredInformationForTheRecallNotification(recallId: RecallId, userId: UserId, inCustody: Boolean) {
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        mappaLevel = MappaLevel.LEVEL_3,
        previousConvictionMainNameCategory = NameFormatCategory.OTHER,
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
        localPoliceForceId = PoliceForceId("greater-manchester"),
        currentPrison = PrisonId("BMI"),
        contraband = true,
        contrabandDetail = "Some contraband detail",
        vulnerabilityDiversity = true,
        vulnerabilityDiversityDetail = "Some stuff",
        assessedByUserId = userId,
        inCustodyAtBooking = inCustody,
        inCustodyAtAssessment = if (inCustody) null else false,
        arrestIssues = !inCustody,
        arrestIssuesDetail = if (inCustody) null else "Some arrest issues",
        lastKnownAddressOption = if (inCustody) null else LastKnownAddressOption.YES
      )
    )
  }
}
