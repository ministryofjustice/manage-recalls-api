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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall

class RandomUtilitiesTest {
  @Test
  fun `can create a random fully populated Recall`() {
    val recall = fullyPopulatedRecall()

    assertThat(
      recall,
      allOf(
        has(Recall::id, present()),
        has(Recall::nomsNumber, present()),
        has("createdByUserId", { it.createdByUserId }, present()),
        has(Recall::createdDateTime, present()),
        has(Recall::lastUpdatedDateTime, present()),
        has(Recall::firstName, present()),
        has(Recall::middleNames, present()),
        has(Recall::lastName, present()),
        has(Recall::licenceNameCategory, present()),
        has(
          Recall::documents,
          allOf(
            hasSize(equalTo(1)),
            allElements(
              allOf(
                has("id", { it.id }, present()),
                has("recallId", { it.recallId }, present()),
                has(Document::category, present()),
                has(Document::fileName, present()),
                has(Document::version, present()),
                has(Document::createdDateTime, present())
              )
            )
          )
        ),
        has(
          Recall::missingDocumentsRecords,
          allOf(
            hasSize(equalTo(1)),
            allElements(
              allOf(
                has("id", { it.id }, present()),
                has("recallId", { it.recallId }, present()),
                has("emailId", { it.emailId }, present()),
                has(MissingDocumentsRecord::detail, present()),
                has(MissingDocumentsRecord::version, present()),
                has(MissingDocumentsRecord::createdByUserId, present()),
                has(MissingDocumentsRecord::createdDateTime, present())
              )
            )
          )
        ),
        has(Recall::recallType, present()),
        has(Recall::recallLength, present()),
        has("lastReleasePrison", { it.lastReleasePrison }, present()),
        has(Recall::lastReleaseDate, present()),
        has(Recall::recallEmailReceivedDateTime, present()),
        has(Recall::localPoliceForce, present()),
        has(Recall::localPoliceForceId, present()),
        has(Recall::contraband, present()),
        has(Recall::contrabandDetail, present()),
        has(Recall::vulnerabilityDiversity, present()),
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
        has("currentPrison", { it.currentPrison }, present()),
        has(Recall::additionalLicenceConditions, present()),
        has(Recall::additionalLicenceConditionsDetail, present()),
        has(Recall::differentNomsNumber, present()),
        has(Recall::differentNomsNumberDetail, present()),
        has(Recall::recallNotificationEmailSentDateTime, present()),
        has(Recall::dossierEmailSentDate, present()),
        has(Recall::previousConvictionMainNameCategory, present()),
        has(Recall::hasDossierBeenChecked, present()),
        has("previousConvictionMainName", { it.previousConvictionMainName }, present()),
        has("assessedByUserId", { it.assessedByUserId }, present()),
        has("bookedByUserId", { it.bookedByUserId }, present()),
        has("dossierCreatedByUserId", { it.dossierCreatedByUserId }, present()),
        has(Recall::dossierTargetDate, present()),
        has("assignee", { it.assignee }, present()),
      )
    )
  }

  @Test
  fun `can create a random fully populated UpdateRecallRequest`() {
    val updateRecallRequest: UpdateRecallRequest = fullyPopulatedInstance()

    assertThat(
      updateRecallRequest,
      allOf(
        has(UpdateRecallRequest::licenseNameCategory, present()),
        has(UpdateRecallRequest::lastReleasePrison, present()),
        has(UpdateRecallRequest::lastReleaseDate, present()),
        has(UpdateRecallRequest::recallEmailReceivedDateTime, present()),
        has(UpdateRecallRequest::localPoliceForce, present()),
        has(UpdateRecallRequest::localPoliceForceId, present()),
        has(UpdateRecallRequest::contraband, present()),
        has(UpdateRecallRequest::contrabandDetail, present()),
        has(UpdateRecallRequest::vulnerabilityDiversity, present()),
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
        has(UpdateRecallRequest::previousConvictionMainNameCategory, present()),
        has(UpdateRecallRequest::hasDossierBeenChecked, present()),
        has(UpdateRecallRequest::previousConvictionMainName, present()),
        has(UpdateRecallRequest::assessedByUserId, present()),
        has(UpdateRecallRequest::bookedByUserId, present()),
        has(UpdateRecallRequest::dossierCreatedByUserId, present()),
      )
    )
  }
}
