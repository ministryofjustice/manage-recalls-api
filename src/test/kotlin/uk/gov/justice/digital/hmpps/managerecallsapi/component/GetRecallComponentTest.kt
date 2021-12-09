package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
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
        LastName("Badger")
      )
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
          now,
          FirstName("Barrie"),
          null,
          LastName("Badger"),
          NameFormatCategory.FIRST_LAST,
          Status.BEING_BOOKED_ON
        )
      )
    )
  }

  @Test
  fun `get a fully populated recall`() {
    val recallId = ::RecallId.random()
    val createdByUserId = authenticatedClient.userId
    val fullyPopulatedRecall = fullyPopulatedRecall(recallId, createdByUserId)
    recallRepository.save(fullyPopulatedRecall)

    val missingDocumentsRecord = fullyPopulatedRecall.missingDocumentsRecords.first()
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
      .jsonPath("$.licenceNameCategory").isEqualTo(fullyPopulatedRecall.licenceNameCategory.toString())
      .jsonPath("$.documents.length()").isEqualTo(1)
      .jsonPath("$.missingDocumentsRecords.length()").isEqualTo(1)
      .jsonPath("$.missingDocumentsRecords[0].missingDocumentsRecordId").isEqualTo(missingDocumentsRecord.id.toString())
      .jsonPath("$.missingDocumentsRecords[0].categories.length()").isEqualTo(missingDocumentsRecord.categories.size)
      .jsonPath("$.missingDocumentsRecords[0].detail").isEqualTo(missingDocumentsRecord.detail)
      .jsonPath("$.missingDocumentsRecords[0].version").isEqualTo(missingDocumentsRecord.version)
      .jsonPath("$.missingDocumentsRecords[0].emailId").isEqualTo(missingDocumentsRecord.emailId.toString())
      .jsonPath("$.missingDocumentsRecords[0].createdByUserId").isEqualTo(missingDocumentsRecord.createdByUserId.toString())
      .jsonPath("$.missingDocumentsRecords[0].createdDateTime").value(endsWith("Z"))
      .jsonPath("$.recallLength").isEqualTo(fullyPopulatedRecall.recallLength!!.name)
      .jsonPath("$.lastReleasePrison").isEqualTo(fullyPopulatedRecall.lastReleasePrison!!.value)
      .jsonPath("$.recallEmailReceivedDateTime").value(endsWith("Z"))
      .jsonPath("$.localPoliceForceId").isEqualTo(fullyPopulatedRecall.localPoliceForceId!!.toString())
      .jsonPath("$.contraband").isEqualTo(fullyPopulatedRecall.contraband!!)
      .jsonPath("$.contrabandDetail").isEqualTo(fullyPopulatedRecall.contrabandDetail!!)
      .jsonPath("$.vulnerabilityDiversity").isEqualTo(fullyPopulatedRecall.vulnerabilityDiversity!!)
      .jsonPath("$.vulnerabilityDiversityDetail").isEqualTo(fullyPopulatedRecall.vulnerabilityDiversityDetail!!)
      .jsonPath("$.mappaLevel").isEqualTo(fullyPopulatedRecall.mappaLevel!!.name)
      .jsonPath("$.sentenceDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.licenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentencingCourt").isEqualTo(fullyPopulatedRecall.sentencingInfo!!.sentencingCourt.value)
      .jsonPath("$.indexOffence").isEqualTo(fullyPopulatedRecall.sentencingInfo!!.indexOffence)
      .jsonPath("$.conditionalReleaseDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.bookingNumber").isEqualTo(fullyPopulatedRecall.bookingNumber!!)
      .jsonPath("$.probationOfficerName").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerName)
      .jsonPath("$.probationOfficerPhoneNumber").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerPhoneNumber)
      .jsonPath("$.probationOfficerEmail").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerEmail)
      .jsonPath("$.localDeliveryUnit").isEqualTo(fullyPopulatedRecall.probationInfo!!.localDeliveryUnit.name)
      .jsonPath("$.authorisingAssistantChiefOfficer").isEqualTo(fullyPopulatedRecall.probationInfo!!.authorisingAssistantChiefOfficer)
      .jsonPath("$.licenceConditionsBreached").isEqualTo(fullyPopulatedRecall.licenceConditionsBreached!!)
      .jsonPath("$.reasonsForRecall.length()").isEqualTo(1)
      .jsonPath("$.reasonsForRecallOtherDetail").isEqualTo(fullyPopulatedRecall.reasonsForRecallOtherDetail!!)
      .jsonPath("$.agreeWithRecall").isEqualTo(fullyPopulatedRecall.agreeWithRecall!!.name)
      .jsonPath("$.agreeWithRecallDetail").isEqualTo(fullyPopulatedRecall.agreeWithRecallDetail!!)
      .jsonPath("$.currentPrison").isEqualTo(fullyPopulatedRecall.currentPrison!!.value)
      .jsonPath("$.recallNotificationEmailSentDateTime").value(endsWith("Z"))
      .jsonPath("$.dossierEmailSentDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.status").isEqualTo(Status.DOSSIER_ISSUED.toString())
      .jsonPath("$.previousConvictionMainNameCategory").isEqualTo(fullyPopulatedRecall.previousConvictionMainNameCategory!!.toString())
      .jsonPath("$.hasDossierBeenChecked").isEqualTo(fullyPopulatedRecall.hasDossierBeenChecked!!)
      .jsonPath("$.previousConvictionMainName").isEqualTo(fullyPopulatedRecall.previousConvictionMainName!!)
      .jsonPath("$.assessedByUserId").isEqualTo(fullyPopulatedRecall.assessedByUserId!!.toString())
      .jsonPath("$.bookedByUserId").isEqualTo(fullyPopulatedRecall.bookedByUserId!!.toString())
      .jsonPath("$.dossierCreatedByUserId").isEqualTo(fullyPopulatedRecall.dossierCreatedByUserId!!.toString())
      .jsonPath("$.recallAssessmentDueDateTime").value(endsWith("Z"))
      .jsonPath("$.dossierTargetDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.assignee").isEqualTo(fullyPopulatedRecall.assignee!!.toString())
      .jsonPath("$.assigneeUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.dossierCreatedByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.bookedByUserName").isEqualTo("Bertie Badger")
      .jsonPath("$.assessedByUserName").isEqualTo("Bertie Badger")
  }

  @Test
  fun `get recall returns 404 if it does not exist`() {
    authenticatedClient.getRecall(::RecallId.random(), expectedStatus = NOT_FOUND)
  }
}
