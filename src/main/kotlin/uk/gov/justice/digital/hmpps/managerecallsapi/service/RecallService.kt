package uk.gov.justice.digital.hmpps.managerecallsapi.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ConfirmedRecallTypeRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.StopRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonApiClient
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.transaction.Transactional

@Service
class RecallService(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val bankHolidayService: BankHolidayService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val prisonApiClient: PrisonApiClient,
  @Autowired private val clock: Clock,
  @Autowired private val meterRegistry: MeterRegistry,
  @Value("\${returnToCustody.updateThresholdMinutes}") val returnToCustodyUpdateThresholdMinutes: Long,
) {

  companion object {
    val SYSTEM_USER_ID = UserId(UUID.fromString("99999999-9999-9999-9999-999999999999"))
  }

  private var rtcCounter: Counter = meterRegistry.counter("autoReturnedToCustody")
  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun assignRecall(recallId: RecallId, assignee: UserId, currentUserId: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .copy(
        assignee = assignee.value
      )
      .let { recallRepository.save(it, currentUserId) }
  }

  @Transactional
  fun unassignRecall(recallId: RecallId, assignee: UserId, currentUserId: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .takeIf { it.assignee == assignee.value }
      ?.copy(
        assignee = null
      )
      ?.let { recallRepository.save(it, currentUserId) } ?: throw NotFoundException()
  }

  @Transactional
  fun updateRecommendedRecallType(recallId: RecallId, recallType: RecallType, currentUserId: UserId): Recall {
    val recall = recallRepository.getByRecallId(recallId)
    return recall.copy(
      recommendedRecallType = recallType,
      recallLength = recall.sentencingInfo?.calculateRecallLength(recallType)
    )
      .let { recallRepository.save(it, currentUserId) }
  }

  @Transactional
  fun confirmRecallType(recallId: RecallId, request: ConfirmedRecallTypeRequest, currentUserId: UserId): Recall {
    val recall = recallRepository.getByRecallId(recallId)
    return recall.copy(
      confirmedRecallType = request.confirmedRecallType,
      confirmedRecallTypeDetail = request.confirmedRecallTypeDetail,
      recallLength = recall.sentencingInfo?.calculateRecallLength(request.confirmedRecallType)
    )
      .let { recallRepository.save(it, currentUserId) }
  }

  @Transactional
  fun updateRecall(recallId: RecallId, updateRecallRequest: UpdateRecallRequest, currentUserId: UserId): Recall =
    recallRepository.getByRecallId(recallId)
      .updateWithRequestDetails(updateRecallRequest)
      .let { recallRepository.save(it, currentUserId) }

  private fun Recall.updateWithRequestDetails(updateRecallRequest: UpdateRecallRequest): Recall {
    val sentencingInfo = updateRecallRequest.toSentencingInfo(this)
    return copy(
      licenceNameCategory = updateRecallRequest.licenceNameCategory ?: licenceNameCategory,
      recallLength = sentencingInfo?.calculateRecallLength(recommendedRecallType),
      recallEmailReceivedDateTime = updateRecallRequest.recallEmailReceivedDateTime ?: recallEmailReceivedDateTime,
      lastReleasePrison = updateRecallRequest.lastReleasePrison ?: lastReleasePrison,
      lastReleaseDate = updateRecallRequest.lastReleaseDate ?: lastReleaseDate,
      localPoliceForceId = updateRecallRequest.localPoliceForceId ?: localPoliceForceId,
      inCustodyAtBooking = updateRecallRequest.inCustodyAtBooking ?: inCustodyAtBooking,
      inCustodyAtAssessment = updateRecallRequest.inCustodyAtAssessment ?: inCustodyAtAssessment,
      contraband = updateRecallRequest.contraband ?: contraband,
      contrabandDetail = updateRecallRequest.contrabandDetail ?: contrabandDetail,
      vulnerabilityDiversity = updateRecallRequest.vulnerabilityDiversity ?: vulnerabilityDiversity,
      vulnerabilityDiversityDetail = updateRecallRequest.vulnerabilityDiversityDetail ?: vulnerabilityDiversityDetail,
      mappaLevel = updateRecallRequest.mappaLevel ?: mappaLevel,
      sentencingInfo = sentencingInfo,
      probationInfo = updateRecallRequest.toProbationInfo(this),
      bookingNumber = updateRecallRequest.bookingNumber ?: bookingNumber,
      licenceConditionsBreached = updateRecallRequest.licenceConditionsBreached ?: licenceConditionsBreached,
      reasonsForRecall = updateRecallRequest.reasonsForRecall ?: reasonsForRecall,
      reasonsForRecallOtherDetail = updateRecallRequest.reasonsForRecallOtherDetail ?: reasonsForRecallOtherDetail,
      agreeWithRecall = updateRecallRequest.agreeWithRecall ?: agreeWithRecall,
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
      dossierTargetDate = calculateDossierTargetDate(updateRecallRequest, this),
      lastKnownAddressOption = updateRecallRequest.lastKnownAddressOption ?: lastKnownAddressOption,
      arrestIssues = updateRecallRequest.arrestIssues ?: arrestIssues,
      arrestIssuesDetail = updateRecallRequest.arrestIssuesDetail ?: arrestIssuesDetail,
      warrantReferenceNumber = updateRecallRequest.warrantReferenceNumber ?: warrantReferenceNumber,
    )
  }

  fun calculateDossierTargetDate(updateRecallRequest: UpdateRecallRequest, recall: Recall): LocalDate? =
    when (recall.inCustodyRecallOrBeingUpdatedToBeElseNull(updateRecallRequest)) {
      true -> {
        updateRecallRequest.recallNotificationEmailSentDateTime?.let {
          bankHolidayService.nextWorkingDate(it.toLocalDate())
        }
      }
      else -> null
    } ?: recall.dossierTargetDate

  @Transactional
  fun updateCustodyStatus(currentUserId: UserId) {
    val rtcRecalls = recallRepository.findAll()
      .filter { it.status() == Status.AWAITING_RETURN_TO_CUSTODY }
      .filter { (0L == returnToCustodyUpdateThresholdMinutes) || it.lastUpdatedDateTime.isBefore(OffsetDateTime.now(clock).minusMinutes(returnToCustodyUpdateThresholdMinutes)) }
      .filter { prisonerOffenderSearchClient.prisonerByNomsNumber(it.nomsNumber).block()!!.isInCustody() }

    if (rtcRecalls.isNotEmpty()) {
      val movements =
        prisonApiClient.latestInboundMovements(rtcRecalls.map { it.nomsNumber }.toSet()).associateBy { it.nomsNumber() }

      rtcRecalls.forEach {
        val returnedToCustodyDateTime = movements[it.nomsNumber]!!.movementDateTime()
        log.info("Returning ${it.recallId()} to custody as of $returnedToCustodyDateTime")
        returnedToCustody(it, returnedToCustodyDateTime, OffsetDateTime.now(clock), SYSTEM_USER_ID)
        rtcCounter.increment()
      }
    }
  }

  @Transactional
  fun manuallyReturnedToCustody(recallId: RecallId, returnedToCustodyDateTime: OffsetDateTime, returnedToCustodyNotificationDateTime: OffsetDateTime, currentUserId: UserId): Recall =
    returnedToCustody(recallRepository.getByRecallId(recallId), returnedToCustodyDateTime, returnedToCustodyNotificationDateTime, currentUserId)

  private fun returnedToCustody(recall: Recall, returnedToCustodyDateTime: OffsetDateTime, returnedToCustodyNotificationDateTime: OffsetDateTime, recordedUserId: UserId): Recall =
    recallRepository.save(
      recall.copy(
        returnedToCustody = ReturnedToCustodyRecord(
          returnedToCustodyDateTime,
          returnedToCustodyNotificationDateTime,
          OffsetDateTime.now(clock),
          recordedUserId
        ),
        dossierTargetDate = bankHolidayService.nextWorkingDate(returnedToCustodyNotificationDateTime.toLocalDate()),
      ),
      recordedUserId
    )

  @Transactional
  fun stopRecall(recallId: RecallId, request: StopRecallRequest, currentUserId: UserId) =
    recallRepository.getByRecallId(recallId).let { recall ->
      if (!request.stopReason.validForStopCall) {
        throw InvalidStopReasonException(recallId, request.stopReason)
      }
      recallRepository.save(
        recall.copy(
          stopRecord = StopRecord(
            request.stopReason,
            currentUserId,
            OffsetDateTime.now(clock)
          ),
          assignee = null
        ),
        currentUserId
      )
    }
}

private fun Prisoner.isInCustody(): Boolean {
  return status?.startsWith("ACTIVE") ?: false
}

private fun UpdateRecallRequest.toSentencingInfo(
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

private fun UpdateRecallRequest.toProbationInfo(existingRecall: Recall): ProbationInfo? =
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
