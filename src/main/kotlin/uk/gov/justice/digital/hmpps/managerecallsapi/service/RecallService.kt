package uk.gov.justice.digital.hmpps.managerecallsapi.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.managerecallsapi.config.InvalidStopReasonException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ConfirmedRecallTypeRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Phase
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StopRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LegalRepresentativeInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SeniorProbationOfficerInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.StopRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.nomis.PrisonApiClient
import uk.gov.justice.digital.hmpps.managerecallsapi.nomis.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.nomis.PrisonerOffenderSearchClient
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
  @Autowired private val phaseRecordService: PhaseRecordService,
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
  fun bookRecall(bookRecallRequest: BookRecallRequest, currentUserId: UserId): Recall {
    val recall = recallRepository.save(bookRecallRequest.toRecall(currentUserId, clock), currentUserId)
    phaseRecordService.startPhase(recall.recallId(), Phase.BOOK, currentUserId)
    return recall
  }

  @Transactional
  fun assignRecall(recallId: RecallId, assignee: UserId, currentUserId: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .copy(
        assignee = assignee.value
      )
      .let { recallRepository.save(it, currentUserId) }
  }

  @Transactional
  fun unassignRecall(recallId: RecallId, currentUserId: UserId): Recall {
    return recallRepository.getByRecallId(recallId)
      .copy(
        assignee = null
      )
      .let { recallRepository.save(it, currentUserId) }
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
      additionalLicenceConditions = updateRecallRequest.additionalLicenceConditions ?: additionalLicenceConditions,
      additionalLicenceConditionsDetail = updateRecallRequest.additionalLicenceConditionsDetail ?: additionalLicenceConditionsDetail,
      arrestIssues = updateRecallRequest.arrestIssues ?: arrestIssues,
      arrestIssuesDetail = updateRecallRequest.arrestIssuesDetail ?: arrestIssuesDetail,
      assessedByUserId = updateRecallRequest.assessedByUserId?.value ?: assessedByUserId,
      bookedByUserId = updateRecallRequest.bookedByUserId?.value ?: bookedByUserId,
      bookingNumber = updateRecallRequest.bookingNumber ?: bookingNumber,
      contraband = updateRecallRequest.contraband ?: contraband,
      contrabandDetail = updateRecallRequest.contrabandDetail ?: contrabandDetail,
      currentPrison = updateRecallRequest.currentPrison ?: currentPrison,
      differentNomsNumber = updateRecallRequest.differentNomsNumber ?: differentNomsNumber,
      differentNomsNumberDetail = updateRecallRequest.differentNomsNumberDetail ?: differentNomsNumberDetail,
      dossierCreatedByUserId = updateRecallRequest.dossierCreatedByUserId?.value ?: dossierCreatedByUserId,
      dossierEmailSentDate = updateRecallRequest.dossierEmailSentDate ?: dossierEmailSentDate,
      dossierTargetDate = calculateDossierTargetDate(updateRecallRequest, this),
      hasDossierBeenChecked = updateRecallRequest.hasDossierBeenChecked ?: hasDossierBeenChecked,
      inCustodyAtAssessment = updateRecallRequest.inCustodyAtAssessment ?: inCustodyAtAssessment,
      inCustodyAtBooking = updateRecallRequest.inCustodyAtBooking ?: inCustodyAtBooking,
      lastKnownAddressOption = updateRecallRequest.lastKnownAddressOption ?: lastKnownAddressOption,
      lastReleaseDate = updateRecallRequest.lastReleaseDate ?: lastReleaseDate,
      lastReleasePrison = updateRecallRequest.lastReleasePrison ?: lastReleasePrison,
      legalRepresentativeInfo = updateRecallRequest.legalRepresentativeInfo?.toDomain() ?: legalRepresentativeInfo,
      licenceConditionsBreached = updateRecallRequest.licenceConditionsBreached ?: licenceConditionsBreached,
      licenceNameCategory = updateRecallRequest.licenceNameCategory ?: licenceNameCategory,
      localPoliceForceId = updateRecallRequest.localPoliceForceId ?: localPoliceForceId,
      mappaLevel = updateRecallRequest.mappaLevel ?: mappaLevel,
      partBDueDate = calculatePartBDueDate(updateRecallRequest, this),
      previousConvictionMainName = updateRecallRequest.previousConvictionMainName ?: previousConvictionMainName,
      previousConvictionMainNameCategory = updateRecallRequest.previousConvictionMainNameCategory ?: previousConvictionMainNameCategory,
      probationInfo = updateRecallRequest.toProbationInfo(this),
      reasonsForRecall = updateRecallRequest.reasonsForRecall ?: reasonsForRecall,
      reasonsForRecallOtherDetail = updateRecallRequest.reasonsForRecallOtherDetail ?: reasonsForRecallOtherDetail,
      recallEmailReceivedDateTime = updateRecallRequest.recallEmailReceivedDateTime ?: recallEmailReceivedDateTime,
      recallLength = sentencingInfo?.calculateRecallLength(recommendedRecallType),
      recallNotificationEmailSentDateTime = updateRecallRequest.recallNotificationEmailSentDateTime ?: recallNotificationEmailSentDateTime,
      rereleaseSupported = updateRecallRequest.rereleaseSupported ?: rereleaseSupported,
      seniorProbationOfficerInfo = updateRecallRequest.seniorProbationOfficerInfo?.toDomain() ?: seniorProbationOfficerInfo,
      sentencingInfo = sentencingInfo,
      secondaryDossierDueDate = calculateSecondaryDossierDueDate(updateRecallRequest, this),
      vulnerabilityDiversity = updateRecallRequest.vulnerabilityDiversity ?: vulnerabilityDiversity,
      vulnerabilityDiversityDetail = updateRecallRequest.vulnerabilityDiversityDetail ?: vulnerabilityDiversityDetail,
      warrantReferenceNumber = updateRecallRequest.warrantReferenceNumber ?: warrantReferenceNumber,
    )
  }

  fun calculateDossierTargetDate(updateRecallRequest: UpdateRecallRequest, recall: Recall): LocalDate? {
    val dossierDate = if (recall.inCustodyRecallOrBeingUpdatedToBe(updateRecallRequest)) {
      updateRecallRequest.recallNotificationEmailSentDateTime?.let {
        bankHolidayService.nextWorkingDate(it.toLocalDate())
      }
    } else null
    return dossierDate ?: recall.dossierTargetDate
  }

  fun calculatePartBDueDate(updateRecallRequest: UpdateRecallRequest, recall: Recall): LocalDate? {
    val partBDueDate = if (recall.recallTypeOrNull() == RecallType.STANDARD && recall.inCustodyRecallOrBeingUpdatedToBe(updateRecallRequest)) {
      updateRecallRequest.recallNotificationEmailSentDateTime?.let {
        bankHolidayService.plusWorkingDays(it.toLocalDate(), 14)
      }
    } else null
    return partBDueDate ?: recall.partBDueDate
  }

  fun calculateSecondaryDossierDueDate(updateRecallRequest: UpdateRecallRequest, recall: Recall): LocalDate? {
    val secondaryDossierDueDate = if (recall.recallTypeOrNull() == RecallType.STANDARD && recall.inCustodyRecallOrBeingUpdatedToBe(updateRecallRequest)) {
      updateRecallRequest.recallNotificationEmailSentDateTime?.toLocalDate()?.plusDays(28)
    } else null
    return secondaryDossierDueDate ?: recall.secondaryDossierDueDate
  }

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
          recordedUserId,
          OffsetDateTime.now(clock)
        ),
        dossierTargetDate = bankHolidayService.nextWorkingDate(returnedToCustodyNotificationDateTime.toLocalDate()),
        partBDueDate = if (recall.recallTypeOrNull() == RecallType.STANDARD) bankHolidayService.plusWorkingDays(returnedToCustodyDateTime.toLocalDate(), 14) else null,
        secondaryDossierDueDate = if (recall.recallTypeOrNull() == RecallType.STANDARD) returnedToCustodyDateTime.toLocalDate().plusDays(28) else null
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

fun BookRecallRequest.toRecall(userUuid: UserId, clock: Clock): Recall {
  val now = OffsetDateTime.now(clock)
  return Recall(
    ::RecallId.random(),
    nomsNumber,
    userUuid,
    now,
    firstName,
    middleNames,
    lastName,
    croNumber,
    dateOfBirth,
    licenceNameCategory = if (middleNames == null) NameFormatCategory.FIRST_LAST else null
  )
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

private fun Api.SeniorProbationOfficerInfo.toDomain(): SeniorProbationOfficerInfo =
  SeniorProbationOfficerInfo(fullName, phoneNumber, email)

private fun Api.LegalRepresentativeInfo.toDomain(): LegalRepresentativeInfo =
  LegalRepresentativeInfo(fullName, phoneNumber, email)
