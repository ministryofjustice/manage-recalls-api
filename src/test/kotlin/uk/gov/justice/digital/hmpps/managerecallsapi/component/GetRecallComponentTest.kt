package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.hamcrest.Matchers.endsWith
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
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
    val fullyPopulatedRecall = recallWithPopulatedFields(recallId, nomsNumber)
    recallRepository.save(fullyPopulatedRecall)

    // TODO:  MD Fix assertions, or move somewhere more sensible.
    authenticatedClient.get("/recalls/$recallId")
      .expectBody()
      .jsonPath("$.recallId").isEqualTo(recallId.toString())
      .jsonPath("$.nomsNumber").isEqualTo(nomsNumber.value)
      .jsonPath("$.documents.length()").isEqualTo(2)
      .jsonPath("$.revocationOrderId").isNotEmpty
      .jsonPath("$.recallLength").isEqualTo(fullyPopulatedRecall.recallLength!!.name)
      .jsonPath("$.lastReleasePrison").isEqualTo(fullyPopulatedRecall.lastReleasePrison!!)
      .jsonPath("$.recallEmailReceivedDateTime").value(endsWith("Z"))
      .jsonPath("$.localPoliceForce").isEqualTo(fullyPopulatedRecall.localPoliceForce!!)
      .jsonPath("$.contrabandDetail").isEqualTo(fullyPopulatedRecall.contrabandDetail!!)
      .jsonPath("$.vulnerabilityDiversityDetail").isEqualTo(fullyPopulatedRecall.vulnerabilityDiversityDetail!!)
      .jsonPath("$.mappaLevel").isEqualTo(MappaLevel.NA.name)
      .jsonPath("$.sentenceDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.licenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentenceExpiryDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.sentencingCourt").isEqualTo(fullyPopulatedRecall.sentencingInfo!!.sentencingCourt)
      .jsonPath("$.indexOffence").isEqualTo(fullyPopulatedRecall.sentencingInfo!!.indexOffence)
      .jsonPath("$.conditionalReleaseDate").isEqualTo(LocalDate.now().toString())
      .jsonPath("$.bookingNumber").isEqualTo(fullyPopulatedRecall.bookingNumber!!)
      .jsonPath("$.probationOfficerName").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerName)
      .jsonPath("$.probationOfficerPhoneNumber")
      .isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerPhoneNumber)
      .jsonPath("$.probationOfficerEmail").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationOfficerEmail)
      .jsonPath("$.probationDivision").isEqualTo(fullyPopulatedRecall.probationInfo!!.probationDivision.name)
      .jsonPath("$.authorisingAssistantChiefOfficer")
      .isEqualTo(fullyPopulatedRecall.probationInfo!!.authorisingAssistantChiefOfficer)
      .jsonPath("$.licenceConditionsBreached").isEqualTo(fullyPopulatedRecall.licenceConditionsBreached!!)
      .jsonPath("$.reasonsForRecall.length()").isEqualTo(ReasonForRecall.values().size)
      .jsonPath("$.reasonsForRecallOtherDetail").isEqualTo(fullyPopulatedRecall.reasonsForRecallOtherDetail!!)
      .jsonPath("$.agreeWithRecall").isEqualTo(fullyPopulatedRecall.agreeWithRecall!!.name)
      .jsonPath("$.agreeWithRecallDetail").isEqualTo(fullyPopulatedRecall.agreeWithRecallDetail!!)
      .jsonPath("$.currentPrison").isEqualTo(fullyPopulatedRecall.currentPrison!!)
      .jsonPath("$.recallNotificationEmailSentDateTime").value(endsWith("Z"))
  }

  @Test
  fun `get recall returns 404 if it does not exist`() {
    authenticatedClient.getRecall(::RecallId.random(), expectedStatus = NOT_FOUND)
  }
}
