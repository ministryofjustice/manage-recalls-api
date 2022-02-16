package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
class RecallService(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val bankHolidayService: BankHolidayService,
  @Autowired private val clock: Clock
) {

  @Transactional
  fun assignRecall(recallId: RecallId, assignee: UserId, currentUserId: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .copy(
        assignee = assignee.value,
        lastUpdatedDateTime = OffsetDateTime.now(clock)
      )
      .let { recallRepository.save(it, currentUserId) }
  }

  @Transactional
  fun unassignRecall(recallId: RecallId, assignee: UserId, currentUserId: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .takeIf { it.assignee == assignee.value }
      ?.copy(
        assignee = null,
        lastUpdatedDateTime = OffsetDateTime.now(clock)
      )
      ?.let { recallRepository.save(it, currentUserId) } ?: throw NotFoundException()
  }

  @Transactional
  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest, currentUserId: UserId): Recall =
    recallRepository.getByRecallId(recallId)
      .updateWithRequestDetails(updateRecallRequest)
      .let { recallRepository.save(it, currentUserId) }

  private fun Recall.updateWithRequestDetails(updateRecallRequest: UpdateRecallRequest): Recall {
    val sentencingInfo = updateRecallRequest.toSentencingInfo(this)
    return copy(
      lastUpdatedDateTime = OffsetDateTime.now(clock),
      licenceNameCategory = updateRecallRequest.licenceNameCategory ?: licenceNameCategory,
      recallType = FIXED,
      recallLength = sentencingInfo?.calculateRecallLength() ?: recallLength,
      recallEmailReceivedDateTime = updateRecallRequest.recallEmailReceivedDateTime
        ?: recallEmailReceivedDateTime,
      lastReleasePrison = updateRecallRequest.lastReleasePrison ?: lastReleasePrison,
      lastReleaseDate = updateRecallRequest.lastReleaseDate ?: lastReleaseDate,
      localPoliceForceId = updateRecallRequest.localPoliceForceId ?: localPoliceForceId,
      inCustodyAtBooking = updateRecallRequest.inCustodyAtBooking ?: inCustodyAtBooking,
      inCustodyAtAssessment = updateRecallRequest.inCustodyAtAssessment ?: inCustodyAtAssessment,
      contraband = updateRecallRequest.contraband ?: contraband,
      contrabandDetail = updateRecallRequest.contrabandDetail ?: contrabandDetail,
      vulnerabilityDiversity = updateRecallRequest.vulnerabilityDiversity
        ?: vulnerabilityDiversity,
      vulnerabilityDiversityDetail = updateRecallRequest.vulnerabilityDiversityDetail
        ?: vulnerabilityDiversityDetail,
      mappaLevel = updateRecallRequest.mappaLevel ?: mappaLevel,
      sentencingInfo = sentencingInfo,
      probationInfo = updateRecallRequest.toProbationInfo(this),
      bookingNumber = updateRecallRequest.bookingNumber ?: bookingNumber,
      licenceConditionsBreached = updateRecallRequest.licenceConditionsBreached ?: licenceConditionsBreached,
      reasonsForRecall = updateRecallRequest.reasonsForRecall ?: reasonsForRecall,
      reasonsForRecallOtherDetail = updateRecallRequest.reasonsForRecallOtherDetail ?: reasonsForRecallOtherDetail,
      agreeWithRecall = updateRecallRequest.agreeWithRecall ?: agreeWithRecall,
      assignee = clearAssigneeIfRecallStopped(updateRecallRequest.agreeWithRecall, assignee),
      agreeWithRecallDetail = updateRecallRequest.agreeWithRecallDetail ?: agreeWithRecallDetail,
      currentPrison = updateRecallRequest.currentPrison ?: currentPrison,
      additionalLicenceConditions = updateRecallRequest.additionalLicenceConditions ?: additionalLicenceConditions,
      additionalLicenceConditionsDetail = updateRecallRequest.additionalLicenceConditionsDetail
        ?: additionalLicenceConditionsDetail,
      differentNomsNumber = updateRecallRequest.differentNomsNumber ?: differentNomsNumber,
      differentNomsNumberDetail = updateRecallRequest.differentNomsNumberDetail ?: differentNomsNumberDetail,
      recallNotificationEmailSentDateTime = updateRecallRequest.recallNotificationEmailSentDateTime
        ?: recallNotificationEmailSentDateTime,
      dossierEmailSentDate = updateRecallRequest.dossierEmailSentDate ?: dossierEmailSentDate,
      previousConvictionMainNameCategory = updateRecallRequest.previousConvictionMainNameCategory
        ?: previousConvictionMainNameCategory,
      hasDossierBeenChecked = updateRecallRequest.hasDossierBeenChecked ?: hasDossierBeenChecked,
      previousConvictionMainName = updateRecallRequest.previousConvictionMainName ?: previousConvictionMainName,
      assessedByUserId = updateRecallRequest.assessedByUserId?.value ?: assessedByUserId,
      bookedByUserId = updateRecallRequest.bookedByUserId?.value ?: bookedByUserId,
      dossierCreatedByUserId = updateRecallRequest.dossierCreatedByUserId?.value ?: dossierCreatedByUserId,
      dossierTargetDate = calculateInCustodyDossierTargetDate(inCustodyRecallOrBeingUpdatedToBeElseNull(updateRecallRequest), updateRecallRequest.recallNotificationEmailSentDateTime)
        ?: dossierTargetDate,
      lastKnownAddressOption = updateRecallRequest.lastKnownAddressOption ?: lastKnownAddressOption,
      arrestIssues = updateRecallRequest.arrestIssues ?: arrestIssues,
      arrestIssuesDetail = updateRecallRequest.arrestIssuesDetail ?: arrestIssuesDetail,
      warrantReferenceNumber = updateRecallRequest.warrantReferenceNumber ?: warrantReferenceNumber,
    )
  }

  fun calculateInCustodyDossierTargetDate(
    inCustodyRecall: Boolean?,
    recallNotificationEmailSentDateTime: OffsetDateTime?
  ): LocalDate? =
    when (inCustodyRecall) {
      true -> {
        recallNotificationEmailSentDateTime?.let {
          bankHolidayService.nextWorkingDate(it.toLocalDate())
        }
      }
      else -> null
    }

  fun clearAssigneeIfRecallStopped(agreeWithRecall: AgreeWithRecall?, assignee: UUID?): UUID? {
    return assignee?.let {
      if (AgreeWithRecall.NO_STOP == agreeWithRecall) {
        null
      } else {
        it
      }
    }
  }
}

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
    localDeliveryUnit != null &&
    authorisingAssistantChiefOfficer != null
  ) {
    ProbationInfo(
      probationOfficerName,
      probationOfficerPhoneNumber,
      probationOfficerEmail,
      localDeliveryUnit,
      authorisingAssistantChiefOfficer,
    )
  } else {
    existingRecall.probationInfo
  }
