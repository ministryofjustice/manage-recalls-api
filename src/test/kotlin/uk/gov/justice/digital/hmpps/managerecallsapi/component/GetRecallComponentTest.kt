package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import recallWithPopulatedFields
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ApiRecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallResponse
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random

class GetRecallComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")

  @Test
  fun `get a recall that has just been booked`() {
    val recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))

    val response = authenticatedGetRequest("/recalls/$recallId")

    assertThat(response, equalTo(RecallResponse(recallId, nomsNumber)))
  }

  @Test
  fun `get a fully populated recall`() {
    val recallId = ::RecallId.random()
    val fullyPopulatedRecall = recallWithPopulatedFields(recallId, nomsNumber)
    recallRepository.save(fullyPopulatedRecall)

    val response = authenticatedGetRequest("/recalls/$recallId")

    // MD:  We could alternatively assert on the contents of the json here (and in all endpoint tests) by writing out
    // the response to a file and asserting that it matches an expected version (obviously would not want to use random
    // test data for that) That way it's very obvious when a response changes, and we're not serializing and
    // deserializing. But possibly that sort of assertion should live in the Controller/endpoint specific unit tests.
    assertThat(
      response,
      allOf(
        has(RecallResponse::recallId, equalTo(recallId)),
        has(RecallResponse::nomsNumber, equalTo(nomsNumber)),
        has(RecallResponse::revocationOrderId, equalTo(fullyPopulatedRecall.revocationOrderId)),
        has(RecallResponse::documents, Matcher(List<ApiRecallDocument>::containsAll, fullyPopulatedRecall.documents.map { ApiRecallDocument(it.id, it.category) })),
        has(RecallResponse::recallLength, equalTo(fullyPopulatedRecall.recallLength)),
        has(RecallResponse::recallEmailReceivedDateTime, present()),
        has(RecallResponse::lastReleasePrison, equalTo(fullyPopulatedRecall.lastReleasePrison)),
        has(RecallResponse::lastReleaseDate, equalTo(fullyPopulatedRecall.lastReleaseDate)),
        has(RecallResponse::localPoliceForce, equalTo(fullyPopulatedRecall.localPoliceForce)),
        has(RecallResponse::contrabandDetail, equalTo(fullyPopulatedRecall.contrabandDetail)),
        has(RecallResponse::vulnerabilityDiversityDetail, equalTo(fullyPopulatedRecall.vulnerabilityDiversityDetail)),
        has(RecallResponse::mappaLevel, equalTo(fullyPopulatedRecall.mappaLevel)),
        has(RecallResponse::sentenceDate, equalTo(fullyPopulatedRecall.sentencingInfo!!.sentenceDate)),
        has(RecallResponse::licenceExpiryDate, equalTo(fullyPopulatedRecall.sentencingInfo!!.licenceExpiryDate)),
        has(RecallResponse::sentenceExpiryDate, equalTo(fullyPopulatedRecall.sentencingInfo!!.sentenceExpiryDate)),
        has(RecallResponse::sentencingCourt, equalTo(fullyPopulatedRecall.sentencingInfo!!.sentencingCourt)),
        has(RecallResponse::indexOffence, equalTo(fullyPopulatedRecall.sentencingInfo!!.indexOffence)),
        has(
          "sentenceLength", { it.sentenceLength!! },
          equalTo(
            Api.SentenceLength(
              fullyPopulatedRecall.sentencingInfo!!.sentenceLength.sentenceYears,
              fullyPopulatedRecall.sentencingInfo!!.sentenceLength.sentenceMonths,
              fullyPopulatedRecall.sentencingInfo!!.sentenceLength.sentenceDays
            )
          )
        ),
        has(RecallResponse::conditionalReleaseDate, equalTo(fullyPopulatedRecall.sentencingInfo!!.conditionalReleaseDate)),
        has(RecallResponse::bookingNumber, equalTo(fullyPopulatedRecall.bookingNumber)),
        has(RecallResponse::probationOfficerName, equalTo(fullyPopulatedRecall.probationInfo!!.probationOfficerName)),
        has(RecallResponse::probationOfficerPhoneNumber, equalTo(fullyPopulatedRecall.probationInfo!!.probationOfficerPhoneNumber)),
        has(RecallResponse::probationOfficerEmail, equalTo(fullyPopulatedRecall.probationInfo!!.probationOfficerEmail)),
        has(RecallResponse::probationDivision, equalTo(fullyPopulatedRecall.probationInfo!!.probationDivision)),
        has(RecallResponse::authorisingAssistantChiefOfficer, equalTo(fullyPopulatedRecall.probationInfo!!.authorisingAssistantChiefOfficer)),
        has(RecallResponse::licenceConditionsBreached, equalTo(fullyPopulatedRecall.licenceConditionsBreached)),
        has(RecallResponse::reasonsForRecall, hasSize(equalTo(ReasonForRecall.values().size))),
        has(RecallResponse::reasonsForRecallOtherDetail, equalTo(fullyPopulatedRecall.reasonsForRecallOtherDetail)),
        has(RecallResponse::agreeWithRecall, equalTo(fullyPopulatedRecall.agreeWithRecall)),
        has(RecallResponse::agreeWithRecallDetail, equalTo(fullyPopulatedRecall.agreeWithRecallDetail)),
        has(RecallResponse::currentPrison, equalTo(fullyPopulatedRecall.currentPrison))
      )
    )
  }
}
