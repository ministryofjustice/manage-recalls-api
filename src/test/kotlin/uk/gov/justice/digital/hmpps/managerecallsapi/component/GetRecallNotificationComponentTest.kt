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
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
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
    updateRecallWithRequiredInformationForTheRecallNotification(recall.recallId, userId, true, FIXED)
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

    val recallDocId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION, FileName("RECALL_NOTIFICATION.pdf"))
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

    authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION, FileName("RECALL_NOTIFICATION.pdf"), "Some detail")

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
    updateRecallWithRequiredInformationForTheRecallNotification(recall.recallId, userId, false, FIXED)

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

    val recallDocId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION, FileName("RECALL_NOTIFICATION.pdf"))
    val recallDoc = authenticatedClient.getDocument(recall.recallId, recallDocId.documentId)

    assertThat(recallDoc.content, equalTo(expectedBase64Pdf))

    val latestRevocationOrder =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, REVOCATION_ORDER)!!
    assertThat(latestRevocationOrder.version, equalTo(1))
    assertThat(latestRevocationOrder.fileName, equalTo(FileName("BADGER NATALIA B1234 REVOCATION ORDER.pdf")))
  }

  @Test
  fun `get recall notification for STANDARD in custody recall includes correct letter to probation`() {
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
    updateRecallWithRequiredInformationForTheRecallNotification(recall.recallId, userId, true, STANDARD)

    gotenbergMockServer.stubGenerateRevocationOrder(expectedPdf, firstName)
    gotenbergMockServer.stubGenerateRecallSummary(expectedPdf, "OFFENDER IS IN CUSTODY")
    gotenbergMockServer.stubGenerateLetterToProbation(expectedPdf, "Standard Recall")

    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
      expectedPdf.decodeToString(),
    )

    val recallDocId = authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION, FileName("RECALL_NOTIFICATION.pdf"))
    val recallDoc = authenticatedClient.getDocument(recall.recallId, recallDocId.documentId)

    assertThat(recallDoc.content, equalTo(expectedBase64Pdf))

    val firstRecallNotification =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, RECALL_NOTIFICATION)!!
    assertThat(firstRecallNotification.version, equalTo(1))
    val firstRevocationOrder =
      documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recall.recallId, REVOCATION_ORDER)!!
    assertThat(firstRevocationOrder.version, equalTo(1))
  }

  private fun updateRecallWithRequiredInformationForTheRecallNotification(
    recallId: RecallId,
    userId: UserId,
    inCustody: Boolean,
    recallType: RecallType
  ) {
    authenticatedClient.updateRecommendedRecallType(recallId, recallType)
    authenticatedClient.updateRecall(
      recallId,
      UpdateRecallRequest(
        arrestIssues = !inCustody,
        arrestIssuesDetail = if (inCustody) null else "Some arrest issues",
        assessedByUserId = userId,
        authorisingAssistantChiefOfficer = "Ms Authoriser",
        bookingNumber = BookingNumber("B1234"),
        contraband = true,
        contrabandDetail = "Some contraband detail",
        currentPrison = PrisonId("BMI"),
        inCustodyAtAssessment = if (inCustody) null else false,
        inCustodyAtBooking = inCustody,
        indexOffence = "Some offence",
        lastKnownAddressOption = if (inCustody) null else LastKnownAddressOption.YES,
        lastReleaseDate = LocalDate.of(2021, 9, 2),
        lastReleasePrison = PrisonId("AKI"),
        licenceExpiryDate = LocalDate.of(2020, 11, 1),
        licenceNameCategory = NameFormatCategory.FIRST_LAST,
        localDeliveryUnit = LocalDeliveryUnit.PS_BARNET,
        localPoliceForceId = PoliceForceId("greater-manchester"),
        mappaLevel = MappaLevel.LEVEL_3,
        previousConvictionMainName = "Bryan Badger",
        previousConvictionMainNameCategory = NameFormatCategory.OTHER,
        probationOfficerEmail = "officer@myprobation.com",
        probationOfficerName = "Mr Probation Officer",
        probationOfficerPhoneNumber = "01234567890",
        reasonsForRecall = setOf(POOR_BEHAVIOUR_FURTHER_OFFENCE),
        sentenceDate = LocalDate.of(2012, 5, 17),
        sentenceExpiryDate = LocalDate.of(2020, 10, 29),
        sentenceLength = Api.SentenceLength(2, 3, 10),
        sentencingCourt = CourtId("ACCRYC"),
        vulnerabilityDiversity = true,
        vulnerabilityDiversityDetail = "Some stuff",
      )
    )
  }
}
