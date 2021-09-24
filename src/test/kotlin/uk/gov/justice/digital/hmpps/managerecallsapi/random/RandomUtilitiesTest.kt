package uk.gov.justice.digital.hmpps.managerecallsapi.random

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument

class RandomUtilitiesTest {
  @Test
  fun `can create a random fully populated Recall`() {
    val recall = fullyPopulatedRecall()

    assertThat(
      recall,
      allOf(
        has(Recall::id, present()),
        has(Recall::nomsNumber, present()),
        has(
          Recall::documents,
          allOf(
            hasSize(equalTo(1)),
            allElements(
              allOf(
                has(RecallDocument::id, present()),
                has(RecallDocument::recallId, present()),
                has(RecallDocument::category, present()),
                has(RecallDocument::fileName, present())
              )
            )
          )
        ),
        has(Recall::recallType, present()),
        has(Recall::recallLength, present()),
        has(Recall::lastReleasePrison, present()),
        has(Recall::lastReleaseDate, present()),
        has(Recall::recallEmailReceivedDateTime, present()),
        has(Recall::localPoliceForce, present()),
        has(Recall::contrabandDetail, present()),
        has(Recall::vulnerabilityDiversityDetail, present()),
        has(Recall::mappaLevel, present()),
        has(Recall::sentencingInfo, present()),
        has(Recall::bookingNumber, present()),
        has(Recall::probationInfo, present()),
        has(Recall::licenceConditionsBreached, present()),
        has(Recall::reasonsForRecall, hasSize(equalTo(1))),
        has(Recall::reasonsForRecallOtherDetail, present()),
        has(Recall::agreeWithRecall, present()),
        has(Recall::agreeWithRecallDetail, present()),
        has(Recall::currentPrison, present()),
        has(Recall::additionalLicenceConditions, present()),
        has(Recall::additionalLicenceConditionsDetail, present()),
        has(Recall::differentNomsNumber, present()),
        has(Recall::differentNomsNumberDetail, present()),
        has(Recall::recallNotificationEmailSentDateTime, present()),
        has(Recall::dossierEmailSentDate, present()),
        has(Recall::hasOtherPreviousConvictionMainName, present()),
        has(Recall::hasDossierBeenChecked, present()),
        has(Recall::previousConvictionMainName, present())
      )
    )
  }

  @Test
  fun `can create a random fully populated UpdateRecallRequest`() {
    val updateRecallRequest: UpdateRecallRequest = fullyPopulatedInstance()

    assertThat(
      updateRecallRequest,
      allOf(
        has(UpdateRecallRequest::lastReleasePrison, present()),
        has(UpdateRecallRequest::lastReleaseDate, present()),
        has(UpdateRecallRequest::recallEmailReceivedDateTime, present()),
        has(UpdateRecallRequest::localPoliceForce, present()),
        has(UpdateRecallRequest::contrabandDetail, present()),
        has(UpdateRecallRequest::vulnerabilityDiversityDetail, present()),
        has(UpdateRecallRequest::mappaLevel, present()),
        has(UpdateRecallRequest::sentenceDate, present()),
        has(UpdateRecallRequest::licenceExpiryDate, present()),
        has(UpdateRecallRequest::sentenceExpiryDate, present()),
        has(UpdateRecallRequest::sentencingCourt, present()),
        has(UpdateRecallRequest::indexOffence, present()),
        has(UpdateRecallRequest::conditionalReleaseDate, present()),
        has(UpdateRecallRequest::sentenceLength, present()),
        has(UpdateRecallRequest::bookingNumber, present()),
        has(UpdateRecallRequest::probationOfficerName, present()),
        has(UpdateRecallRequest::probationOfficerPhoneNumber, present()),
        has(UpdateRecallRequest::probationOfficerEmail, present()),
        has(UpdateRecallRequest::localDeliveryUnit, present()),
        has(UpdateRecallRequest::authorisingAssistantChiefOfficer, present()),
        has(UpdateRecallRequest::licenceConditionsBreached, present()),
        has(UpdateRecallRequest::reasonsForRecall, present()),
        has(UpdateRecallRequest::reasonsForRecallOtherDetail, present()),
        has(UpdateRecallRequest::agreeWithRecall, present()),
        has(UpdateRecallRequest::agreeWithRecallDetail, present()),
        has(UpdateRecallRequest::currentPrison, present()),
        has(UpdateRecallRequest::additionalLicenceConditions, present()),
        has(UpdateRecallRequest::additionalLicenceConditionsDetail, present()),
        has(UpdateRecallRequest::differentNomsNumber, present()),
        has(UpdateRecallRequest::differentNomsNumberDetail, present()),
        has(UpdateRecallRequest::recallNotificationEmailSentDateTime, present()),
        has(UpdateRecallRequest::dossierEmailSentDate, present()),
        has(UpdateRecallRequest::hasOtherPreviousConvictionMainName, present()),
        has(UpdateRecallRequest::hasDossierBeenChecked, present()),
        has(UpdateRecallRequest::previousConvictionMainName, present()),
      )
    )
  }
}
