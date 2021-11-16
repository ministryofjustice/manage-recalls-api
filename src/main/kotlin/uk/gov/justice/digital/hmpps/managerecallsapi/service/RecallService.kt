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
import java.time.DayOfWeek
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
  fun assignRecall(recallId: RecallId, assignee: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .copy(
        assignee = assignee.value,
        lastUpdatedDateTime = OffsetDateTime.now(clock)
      )
      .let { recallRepository.save(it) }
  }

  @Transactional
  fun unassignRecall(recallId: RecallId, assignee: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .takeIf { it.assignee == assignee.value }
      ?.copy(
        assignee = null,
        lastUpdatedDateTime = OffsetDateTime.now(clock)
      )
      ?.let { recallRepository.save(it) } ?: throw NotFoundException()
  }

  @Transactional
  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest): Recall =
    recallRepository.getByRecallId(recallId)
      .updateWithRequestDetails(updateRecallRequest)
      .let(recallRepository::save)

  private fun Recall.updateWithRequestDetails(updateRecallRequest: UpdateRecallRequest): Recall {
    val sentencingInfo = updateRecallRequest.toSentencingInfo(this)
    return copy(
      lastUpdatedDateTime = OffsetDateTime.now(clock),
      recallType = FIXED,
      recallLength = sentencingInfo?.calculateRecallLength() ?: recallLength,
      recallEmailReceivedDateTime = updateRecallRequest.recallEmailReceivedDateTime
        ?: recallEmailReceivedDateTime,
      lastReleasePrison = updateRecallRequest.lastReleasePrison ?: lastReleasePrison,
      lastReleaseDate = updateRecallRequest.lastReleaseDate ?: lastReleaseDate,
      localPoliceForce = updateRecallRequest.localPoliceForce ?: localPoliceForce,
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
      additionalLicenceConditionsDetail = updateRecallRequest.additionalLicenceConditionsDetail ?: additionalLicenceConditionsDetail,
      differentNomsNumber = updateRecallRequest.differentNomsNumber ?: differentNomsNumber,
      differentNomsNumberDetail = updateRecallRequest.differentNomsNumberDetail ?: differentNomsNumberDetail,
      recallNotificationEmailSentDateTime = updateRecallRequest.recallNotificationEmailSentDateTime ?: recallNotificationEmailSentDateTime,
      dossierEmailSentDate = updateRecallRequest.dossierEmailSentDate ?: dossierEmailSentDate,
      previousConvictionMainNameCategory = updateRecallRequest.previousConvictionMainNameCategory ?: previousConvictionMainNameCategory,
      hasDossierBeenChecked = updateRecallRequest.hasDossierBeenChecked ?: hasDossierBeenChecked,
      previousConvictionMainName = updateRecallRequest.previousConvictionMainName ?: previousConvictionMainName,
      assessedByUserId = updateRecallRequest.assessedByUserId?.value ?: assessedByUserId,
      bookedByUserId = updateRecallRequest.bookedByUserId?.value ?: bookedByUserId,
      dossierCreatedByUserId = updateRecallRequest.dossierCreatedByUserId?.value ?: dossierCreatedByUserId,
      dossierTargetDate = calculateDossierTargetDate(updateRecallRequest.recallNotificationEmailSentDateTime) ?: dossierTargetDate
    )
  }

  fun calculateDossierTargetDate(recallNotificationEmailSentDateTime: OffsetDateTime?): LocalDate? {
    return recallNotificationEmailSentDateTime?.let {
      var dossierTargetDate = it.toLocalDate().plusDays(1)
      while (dossierTargetDate.dayOfWeek == DayOfWeek.SATURDAY || dossierTargetDate.dayOfWeek == DayOfWeek.SUNDAY || bankHolidayService.isHoliday(dossierTargetDate)) {
        dossierTargetDate = dossierTargetDate.plusDays(1)
      }
      dossierTargetDate
    }
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
