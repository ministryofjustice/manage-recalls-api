package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.fullyPopulatedRecall
import java.time.LocalDate

class GetRecallComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  @Test
  fun `get a recall that has just been booked`() {
    val recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))

    val response = authenticatedClient.getRecall(recallId)

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber)))
  }

  @Test
  fun `get a fully populated recall`() {
    val recallId = ::RecallId.random()
    val fullyPopulatedRecall = fullyPopulatedRecall(recallId)
    userDetailsRepository.save(UserDetails(fullyPopulatedRecall.assignee()!!, FirstName("Bertie"), LastName("Badger"), "", Email("b@b.com"), PhoneNumber("0987654321")))
    recallRepository.save(fullyPopulatedRecall)

    // TODO:  MD Fix assertions, or move somewhere more sensible.
    authenticatedClient.get("/recalls/$recallId")
      .expectBody()
      .jsonPath("$.recallId").isEqualTo(recallId.toString())
      .jsonPath("$.nomsNumber").isEqualTo(fullyPopulatedRecall.nomsNumber.value)
      .jsonPath("$.documents.length()").isEqualTo(1)
      .jsonPath("$.recallLength").isEqualTo(fullyPopulatedRecall.recallLength!!.name)
      .jsonPath("$.lastReleasePrison").isEqualTo(fullyPopulatedRecall.lastReleasePrison!!.value)
      .jsonPath("$.recallEmailReceivedDateTime").value(endsWith("Z"))
      .jsonPath("$.localPoliceForce").isEqualTo(fullyPopulatedRecall.localPoliceForce!!)
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
      .jsonPath("$.probationOfficerPhoneNumber")
      .isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerPhoneNumber)
      .jsonPath("$.probationOfficerEmail").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerEmail)
      .jsonPath("$.localDeliveryUnit").isEqualTo(fullyPopulatedRecall.probationInfo!!.localDeliveryUnit.name)
      .jsonPath("$.authorisingAssistantChiefOfficer")
      .isEqualTo(fullyPopulatedRecall.probationInfo!!.authorisingAssistantChiefOfficer)
      .jsonPath("$.licenceConditionsBreached").isEqualTo(fullyPopulatedRecall.licenceConditionsBreached!!)
      .jsonPath("$.reasonsForRecall.length()").isEqualTo(1)
      .jsonPath("$.reasonsForRecallOtherDetail").isEqualTo(fullyPopulatedRecall.reasonsForRecallOtherDetail!!)
      .jsonPath("$.agreeWithRecall").isEqualTo(fullyPopulatedRecall.agreeWithRecall!!.name)
      .jsonPath("$.agreeWithRecallDetail").isEqualTo(fullyPopulatedRecall.agreeWithRecallDetail!!)
      .jsonPath("$.currentPrison").isEqualTo(fullyPopulatedRecall.currentPrison!!.value)
      .jsonPath("$.recallNotificationEmailSentDateTime").value(endsWith("Z"))
      .jsonPath("$.dossierEmailSentDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.status").isEqualTo(Status.DOSSIER_ISSUED.toString())
      .jsonPath("$.hasOtherPreviousConvictionMainName").isEqualTo(fullyPopulatedRecall.hasOtherPreviousConvictionMainName!!)
      .jsonPath("$.hasDossierBeenChecked").isEqualTo(fullyPopulatedRecall.hasDossierBeenChecked!!)
      .jsonPath("$.previousConvictionMainName").isEqualTo(fullyPopulatedRecall.previousConvictionMainName!!)
      .jsonPath("$.assessedByUserId").isEqualTo(fullyPopulatedRecall.assessedByUserId!!.toString())
      .jsonPath("$.bookedByUserId").isEqualTo(fullyPopulatedRecall.bookedByUserId!!.toString())
      .jsonPath("$.dossierCreatedByUserId").isEqualTo(fullyPopulatedRecall.dossierCreatedByUserId!!.toString())
      .jsonPath("$.recallAssessmentDueDateTime").value(endsWith("Z"))
      .jsonPath("$.dossierTargetDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.assignee").isEqualTo(fullyPopulatedRecall.assignee!!.toString())
      .jsonPath("$.assigneeUserName").isEqualTo("Bertie Badger")
  }

  @Test
  fun `get recall returns 404 if it does not exist`() {
    authenticatedClient.getRecall(::RecallId.random(), expectedStatus = NOT_FOUND)
  }
}
