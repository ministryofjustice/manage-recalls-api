package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallReason
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId

@Service
class UpdateRecallService(private val recallRepository: RecallRepository) {
  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): Recall =
    recallRepository.getByRecallId(recallId)
      .updateWithRequestDetails(updateRecallRequest)
      .let(recallRepository::save)

  private fun Recall.updateWithRequestDetails(updateRecallRequest: UpdateRecallRequest): Recall {
    val sentencingInfo = updateRecallRequest.toSentencingInfo(this)
    return copy(
      recallType = FIXED,
      agreeWithRecallRecommendation = updateRecallRequest.agreeWithRecallRecommendation
        ?: agreeWithRecallRecommendation,
      recallLength = sentencingInfo?.calculateRecallLength() ?: recallLength,
      recallEmailReceivedDateTime = updateRecallRequest.recallEmailReceivedDateTime
        ?: recallEmailReceivedDateTime,
      lastReleasePrison = updateRecallRequest.lastReleasePrison ?: lastReleasePrison,
      lastReleaseDate = updateRecallRequest.lastReleaseDate ?: lastReleaseDate,
      localPoliceForce = updateRecallRequest.localPoliceForce ?: localPoliceForce,
      contrabandDetail = updateRecallRequest.contrabandDetail ?: contrabandDetail,
      vulnerabilityDiversityDetail = updateRecallRequest.vulnerabilityDiversityDetail
        ?: vulnerabilityDiversityDetail,
      mappaLevel = updateRecallRequest.mappaLevel ?: mappaLevel,
      sentencingInfo = sentencingInfo,
      probationInfo = updateRecallRequest.toProbationInfo(this),
      bookingNumber = updateRecallRequest.bookingNumber ?: bookingNumber,
      licenceConditionsBreached = updateRecallRequest.licenceConditionsBreached ?: licenceConditionsBreached,
      reasonsForRecall = updateRecallRequest.toReasonsForRecall(this)
    )
  }
}

fun UpdateRecallRequest.toReasonsForRecall(
  existingRecall: Recall
) =
  reasonsForRecall?.map { RecallReason(it.reasonId, existingRecall.id, it.reasonForRecall) }?.toSet()
    ?: existingRecall.reasonsForRecall

fun UpdateRecallRequest.toSentencingInfo(
  existingRecall: Recall
): SentencingInfo? =
  if (
    sentenceDate != null &&
    licenceExpiryDate != null &&
    sentenceExpiryDate != null &&
    sentencingCourt != null &&
    indexOffence != null &&
    sentenceLength != null
  ) {
    SentencingInfo(
      sentenceDate,
      licenceExpiryDate,
      sentenceExpiryDate,
      sentencingCourt,
      indexOffence,
      SentenceLength(sentenceLength.years, sentenceLength.months, sentenceLength.days),
      conditionalReleaseDate
    )
  } else {
    existingRecall.sentencingInfo
  }

fun UpdateRecallRequest.toProbationInfo(existingRecall: Recall): ProbationInfo? =
  if (
    probationOfficerName != null &&
    probationOfficerPhoneNumber != null &&
    probationOfficerEmail != null &&
    probationDivision != null &&
    authorisingAssistantChiefOfficer != null
  ) {
    ProbationInfo(
      probationOfficerName,
      probationOfficerPhoneNumber,
      probationOfficerEmail,
      probationDivision,
      authorisingAssistantChiefOfficer,
    )
  } else {
    existingRecall.probationInfo
  }
