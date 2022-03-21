package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecall
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class GetRecallComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  @Test
  fun `get a recall that has just been booked`() {
    val recallId = ::RecallId.random()
    val createdByUserId = authenticatedClient.userId

    val now = OffsetDateTime.ofInstant(Instant.parse("2021-10-04T14:15:43.682078Z"), ZoneId.of("UTC"))
    recallRepository.save(
      Recall(
        recallId,
        nomsNumber,
        createdByUserId,
        now,
        FirstName("Barrie"),
        null,
        LastName("Badger"),
        CroNumber("ABC/1234A"),
        LocalDate.of(1999, 12, 1)
      ),
      createdByUserId
    )

    val response = authenticatedClient.getRecall(recallId)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          fixedClockTime,
          FirstName("Barrie"),
          null,
          LastName("Badger"),
          CroNumber("ABC/1234A"),
          LocalDate.of(1999, 12, 1),
          Status.BEING_BOOKED_ON,
        )
      )
    )
  }

  @Test
  fun `get a fully populated FIXED term recall`() {
    val recallId = ::RecallId.random()
    val createdByUserId = authenticatedClient.userId
    val fullyPopulatedRecall = fullyPopulatedRecall(recallId, createdByUserId, RecallType.FIXED)
    recallRepository.save(fullyPopulatedRecall, createdByUserId)

    val document = fullyPopulatedRecall.documents.first()
    val missingDocumentsRecord = fullyPopulatedRecall.missingDocumentsRecords.first()
    val partBRecord = fullyPopulatedRecall.partBRecords.first()
    val lastKnownAddress = fullyPopulatedRecall.lastKnownAddresses.first()
    val rescindRecord = fullyPopulatedRecall.rescindRecords.first()
    val note = fullyPopulatedRecall.notes.first()

    // TODO:  MD Fix assertions, or move somewhere more sensible.
    authenticatedClient.get("/recalls/$recallId")
      .expectBody()
      .jsonPath("$.recallId").isEqualTo(recallId.toString())
      .jsonPath("$.nomsNumber").isEqualTo(fullyPopulatedRecall.nomsNumber.value)
      .jsonPath("$.createdByUserId").isEqualTo(fullyPopulatedRecall.createdByUserId.toString())
      .jsonPath("$.createdDateTime").value(endsWith("Z"))
      .jsonPath("$.lastUpdatedDateTime").value(endsWith("Z"))
      .jsonPath("$.firstName").isEqualTo(fullyPopulatedRecall.firstName.toString())
      .jsonPath("$.middleNames").isEqualTo(fullyPopulatedRecall.middleNames.toString())
      .jsonPath("$.lastName").isEqualTo(fullyPopulatedRecall.lastName.toString())
      .jsonPath("$.croNumber").isEqualTo(fullyPopulatedRecall.croNumber.toString())
      .jsonPath("$.dateOfBirth").isEqualTo(fullyPopulatedRecall.dateOfBirth.toString())
      .jsonPath("$.licenceNameCategory").isEqualTo(fullyPopulatedRecall.licenceNameCategory.toString())
      .jsonPath("$.documents.length()").isEqualTo(1)
      .jsonPath("$.documents[0].documentId").isEqualTo(document.id.toString())
      .jsonPath("$.documents[0].category").isEqualTo(document.category.name)
      .jsonPath("$.documents[0].fileName").isEqualTo(document.fileName.value)
      .jsonPath("$.documents[0].details").isEqualTo(document.details!!)
      .jsonPath("$.documents[0].createdByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.documents[0].createdDateTime").value(endsWith("Z"))
      .jsonPath("$.missingDocumentsRecords.length()").isEqualTo(1)
      .jsonPath("$.missingDocumentsRecords[0].missingDocumentsRecordId").isEqualTo(missingDocumentsRecord.id.toString())
      .jsonPath("$.missingDocumentsRecords[0].categories.length()").isEqualTo(missingDocumentsRecord.categories.size)
      .jsonPath("$.missingDocumentsRecords[0].details").isEqualTo(missingDocumentsRecord.details)
      .jsonPath("$.missingDocumentsRecords[0].version").isEqualTo(missingDocumentsRecord.version)
      .jsonPath("$.missingDocumentsRecords[0].emailId").isEqualTo(missingDocumentsRecord.emailId.toString())
      .jsonPath("$.missingDocumentsRecords[0].createdByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.missingDocumentsRecords[0].createdDateTime").value(endsWith("Z"))
      .jsonPath("$.partBRecords.length()").isEqualTo(1)
      .jsonPath("$.partBRecords[0].partBRecordId").isEqualTo(partBRecord.id.toString())
      .jsonPath("$.partBRecords[0].details").isEqualTo(partBRecord.details)
      .jsonPath("$.partBRecords[0].partBReceivedDate").isEqualTo(partBRecord.partBReceivedDate.toString())
      .jsonPath("$.partBRecords[0].partBDocumentId").isEqualTo(partBRecord.partBDocumentId.toString())
      .jsonPath("$.partBRecords[0].emailId").isEqualTo(partBRecord.emailId.toString())
      .jsonPath("$.partBRecords[0].oasysDocumentId").isEqualTo(partBRecord.oasysDocumentId!!.toString())
      .jsonPath("$.partBRecords[0].version").isEqualTo(partBRecord.version)
      .jsonPath("$.partBRecords[0].createdByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.partBRecords[0].createdDateTime").value(endsWith("Z"))
      .jsonPath("$.notes.length()").isEqualTo(1)
      .jsonPath("$.notes[0].noteId").isEqualTo(note.id.toString())
      .jsonPath("$.notes[0].subject").isEqualTo(note.subject)
      .jsonPath("$.notes[0].details").isEqualTo(note.details)
      .jsonPath("$.notes[0].index").isEqualTo(note.index)
      .jsonPath("$.notes[0].documentId").isEqualTo(note.documentId!!.toString())
      .jsonPath("$.notes[0].createdByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.notes[0].createdDateTime").value(endsWith("Z"))
      .jsonPath("$.lastKnownAddresses.length()").isEqualTo(1)
      .jsonPath("$.lastKnownAddresses[0].lastKnownAddressId").isEqualTo(lastKnownAddress.id.toString())
      .jsonPath("$.lastKnownAddresses[0].line1").isEqualTo(lastKnownAddress.line1)
      .jsonPath("$.lastKnownAddresses[0].line2").isEqualTo(lastKnownAddress.line2!!)
      .jsonPath("$.lastKnownAddresses[0].town").isEqualTo(lastKnownAddress.town)
      .jsonPath("$.lastKnownAddresses[0].postcode").isEqualTo(lastKnownAddress.postcode!!)
      .jsonPath("$.lastKnownAddresses[0].index").isEqualTo(lastKnownAddress.index)
      .jsonPath("$.lastKnownAddresses[0].source").isEqualTo(lastKnownAddress.source.name)
      .jsonPath("$.lastKnownAddresses[0].createdByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.lastKnownAddresses[0].createdDateTime").value(endsWith("Z"))
      .jsonPath("$.rescindRecords.length()").isEqualTo(1)
      .jsonPath("$.rescindRecords[0].rescindRecordId").isEqualTo(rescindRecord.id.toString())
      .jsonPath("$.rescindRecords[0].version").isEqualTo(rescindRecord.version)
      .jsonPath("$.rescindRecords[0].requestDetails").isEqualTo(rescindRecord.requestDetails)
      .jsonPath("$.rescindRecords[0].requestEmailId").isEqualTo(rescindRecord.requestEmailId.toString())
      .jsonPath("$.rescindRecords[0].requestEmailReceivedDate").isEqualTo(rescindRecord.requestEmailReceivedDate.toString())
      .jsonPath("$.rescindRecords[0].approved").isEqualTo(rescindRecord.approved!!)
      .jsonPath("$.rescindRecords[0].decisionDetails").isEqualTo(rescindRecord.decisionDetails!!)
      .jsonPath("$.rescindRecords[0].decisionEmailId").isEqualTo(rescindRecord.decisionEmailId!!.toString())
      .jsonPath("$.rescindRecords[0].decisionEmailSentDate").isEqualTo(rescindRecord.decisionEmailSentDate!!.toString())
      .jsonPath("$.rescindRecords[0].createdByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.rescindRecords[0].createdDateTime").value(endsWith("Z"))
      .jsonPath("$.rescindRecords[0].lastUpdatedDateTime").value(endsWith("Z"))
      .jsonPath("$.arrestIssues").isEqualTo(fullyPopulatedRecall.arrestIssues!!)
      .jsonPath("$.arrestIssuesDetail").isEqualTo(fullyPopulatedRecall.arrestIssuesDetail!!)
      .jsonPath("$.assessedByUserId").isEqualTo(fullyPopulatedRecall.assessedByUserId!!.toString())
      .jsonPath("$.assessedByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.assignee").isEqualTo(fullyPopulatedRecall.assignee!!.toString())
      .jsonPath("$.assigneeUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.authorisingAssistantChiefOfficer").isEqualTo(fullyPopulatedRecall.probationInfo!!.authorisingAssistantChiefOfficer)
      .jsonPath("$.bookedByUserId").isEqualTo(fullyPopulatedRecall.bookedByUserId!!.toString())
      .jsonPath("$.bookedByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.bookingNumber").isEqualTo(fullyPopulatedRecall.bookingNumber!!.value)
      .jsonPath("$.conditionalReleaseDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.confirmedRecallType").isEqualTo(fullyPopulatedRecall.confirmedRecallType!!.name)
      .jsonPath("$.confirmedRecallTypeDetail").isEqualTo(fullyPopulatedRecall.confirmedRecallTypeDetail!!)
      .jsonPath("$.contraband").isEqualTo(fullyPopulatedRecall.contraband!!)
      .jsonPath("$.contrabandDetail").isEqualTo(fullyPopulatedRecall.contrabandDetail!!)
      .jsonPath("$.currentPrison").isEqualTo(fullyPopulatedRecall.currentPrison!!.value)
      .jsonPath("$.dossierCreatedByUserId").isEqualTo(fullyPopulatedRecall.dossierCreatedByUserId!!.toString())
      .jsonPath("$.dossierCreatedByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.dossierEmailSentDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.dossierTargetDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.hasDossierBeenChecked").isEqualTo(fullyPopulatedRecall.hasDossierBeenChecked!!)
      .jsonPath("$.inCustodyAtAssessment").isEqualTo(fullyPopulatedRecall.inCustodyAtAssessment!!)
      .jsonPath("$.inCustodyAtBooking").isEqualTo(fullyPopulatedRecall.inCustodyAtBooking!!)
      .jsonPath("$.indexOffence").isEqualTo(fullyPopulatedRecall.sentencingInfo!!.indexOffence)
      .jsonPath("$.lastKnownAddressOption").isEqualTo(fullyPopulatedRecall.lastKnownAddressOption!!.toString())
      .jsonPath("$.lastReleasePrison").isEqualTo(fullyPopulatedRecall.lastReleasePrison!!.value)
      .jsonPath("$.licenceConditionsBreached").isEqualTo(fullyPopulatedRecall.licenceConditionsBreached!!)
      .jsonPath("$.licenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.localDeliveryUnit").isEqualTo(fullyPopulatedRecall.probationInfo!!.localDeliveryUnit.name)
      .jsonPath("$.localPoliceForceId").isEqualTo(fullyPopulatedRecall.localPoliceForceId!!.toString())
      .jsonPath("$.mappaLevel").isEqualTo(fullyPopulatedRecall.mappaLevel!!.name)
      .jsonPath("$.partBDueDate").isEqualTo(fullyPopulatedRecall.partBDueDate!!.toString())
      .jsonPath("$.previousConvictionMainName").isEqualTo(fullyPopulatedRecall.previousConvictionMainName!!)
      .jsonPath("$.previousConvictionMainNameCategory").isEqualTo(fullyPopulatedRecall.previousConvictionMainNameCategory!!.toString())
      .jsonPath("$.probationOfficerEmail").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerEmail)
      .jsonPath("$.probationOfficerName").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerName)
      .jsonPath("$.probationOfficerPhoneNumber").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerPhoneNumber)
      .jsonPath("$.reasonsForRecall.length()").isEqualTo(1)
      .jsonPath("$.reasonsForRecallOtherDetail").isEqualTo(fullyPopulatedRecall.reasonsForRecallOtherDetail!!)
      .jsonPath("$.recallAssessmentDueDateTime").value(endsWith("Z"))
      .jsonPath("$.recallEmailReceivedDateTime").value(endsWith("Z"))
      .jsonPath("$.recallLength").isEqualTo(fullyPopulatedRecall.recallLength!!.name)
      .jsonPath("$.recallNotificationEmailSentDateTime").value(endsWith("Z"))
      .jsonPath("$.recommendedRecallType").isEqualTo(fullyPopulatedRecall.recommendedRecallType!!.name)
      .jsonPath("$.returnedToCustodyDateTime").value(endsWith("Z"))
      .jsonPath("$.returnedToCustodyNotificationDateTime").value(endsWith("Z"))
      .jsonPath("$.sentenceDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentencingCourt").isEqualTo(fullyPopulatedRecall.sentencingInfo!!.sentencingCourt.value)
      .jsonPath("$.status").isEqualTo(Status.STOPPED.toString())
      .jsonPath("$.stopByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.stopDateTime").value(endsWith("Z"))
      .jsonPath("$.stopReason").isEqualTo(fullyPopulatedRecall.stopRecord!!.stopReason.toString())
      .jsonPath("$.vulnerabilityDiversity").isEqualTo(fullyPopulatedRecall.vulnerabilityDiversity!!)
      .jsonPath("$.vulnerabilityDiversityDetail").isEqualTo(fullyPopulatedRecall.vulnerabilityDiversityDetail!!)
      .jsonPath("$.warrantReferenceNumber").isEqualTo(fullyPopulatedRecall.warrantReferenceNumber!!.value)
  }

  @Test
  fun `get recall returns 404 if it does not exist`() {
    authenticatedClient.getRecall(::RecallId.random(), expectedStatus = NOT_FOUND)
  }
}
