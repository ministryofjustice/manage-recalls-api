package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LastKnownAddressOption
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.FIRST_MIDDLE_LAST
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Status
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.StoppedReason
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.CascadeType.ALL
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.ElementCollection
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "recall")
data class Recall(

// NON-NULLABLE FIELDS
  @Id
  val id: UUID,
  @Column(nullable = false)
  @Convert(converter = NomsNumberJpaConverter::class)
  val nomsNumber: NomsNumber,
  @Column(nullable = false)
  val createdByUserId: UUID,
  @Column(nullable = false)
  val createdDateTime: OffsetDateTime,
  // MD: ideally this (and others) would be UserId, but hibernate/postgres does not make this easy :-(
  val lastUpdatedByUserId: UUID,
  @Column(nullable = false)
  val lastUpdatedDateTime: OffsetDateTime,
  @Column(nullable = false)
  @Convert(converter = FirstNameJpaConverter::class)
  val firstName: FirstName,
  @Convert(converter = MiddleNamesJpaConverter::class)
  val middleNames: MiddleNames?,
  @Column(nullable = false)
  @Convert(converter = LastNameJpaConverter::class)
  val lastName: LastName,
  @Column(nullable = false)
  @Convert(converter = CroNumberJpaConverter::class)
  val croNumber: CroNumber,
  @Column(nullable = false)
  val dateOfBirth: LocalDate,
  @Column(nullable = false)
  @Enumerated(STRING)
  val licenceNameCategory: NameFormatCategory,

// COLLECTIONS
  @OneToMany(cascade = [ALL])
  @JoinColumn(name = "recall_id")
  val documents: Set<Document> = emptySet(),
  @OneToMany(cascade = [ALL])
  @JoinColumn(name = "recall_id")
  val lastKnownAddresses: Set<LastKnownAddress> = emptySet(),
  @OneToMany(cascade = [ALL])
  @JoinColumn(name = "recall_id")
  val missingDocumentsRecords: Set<MissingDocumentsRecord> = emptySet(),
  @ElementCollection(targetClass = ReasonForRecall::class)
  @JoinTable(name = "recall_reason", joinColumns = [JoinColumn(name = "recall_id")])
  @Column(name = "reason_for_recall", nullable = false)
  @Enumerated(STRING)
  val reasonsForRecall: Set<ReasonForRecall> = emptySet(),
  @OneToMany(cascade = [ALL])
  @JoinColumn(name = "recall_id")
  val rescindRecords: Set<RescindRecord> = emptySet(),

// NULLABLE FIELDS
  val additionalLicenceConditions: Boolean? = null,
  val additionalLicenceConditionsDetail: String? = null,
  @Enumerated(STRING)
  val agreeWithRecall: AgreeWithRecall? = null,
  val agreeWithRecallDetail: String? = null,
  val arrestIssues: Boolean? = null,
  val arrestIssuesDetail: String? = null,
  val assessedByUserId: UUID? = null,
  val assignee: UUID? = null,
  val bookedByUserId: UUID? = null,
  val bookingNumber: String? = null,
  val contraband: Boolean? = null,
  val contrabandDetail: String? = null,
  @Convert(converter = PrisonIdJpaConverter::class)
  val currentPrison: PrisonId? = null,
  val differentNomsNumber: Boolean? = null,
  val differentNomsNumberDetail: String? = null,
  val dossierCreatedByUserId: UUID? = null,
  val dossierEmailSentDate: LocalDate? = null,
  val dossierTargetDate: LocalDate? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val inCustodyAtBooking: Boolean? = null,
  val inCustodyAtAssessment: Boolean? = null,
  @Enumerated(STRING)
  val lastKnownAddressOption: LastKnownAddressOption? = null,
  val lastReleaseDate: LocalDate? = null,
  @Convert(converter = PrisonIdJpaConverter::class)
  val lastReleasePrison: PrisonId? = null,
  val licenceConditionsBreached: String? = null,
  @Convert(converter = PoliceForceIdJpaConverter::class)
  val localPoliceForceId: PoliceForceId? = null,
  @Enumerated(STRING)
  val mappaLevel: MappaLevel? = null,
  val previousConvictionMainName: String? = null,
  @Enumerated(STRING)
  val previousConvictionMainNameCategory: NameFormatCategory? = null,
  @Embedded
  val probationInfo: ProbationInfo? = null,
  val reasonsForRecallOtherDetail: String? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  @Enumerated(STRING)
  val recallLength: RecallLength? = null,
  val recallNotificationEmailSentDateTime: OffsetDateTime? = null,
  @Enumerated(STRING)
  val recallType: RecallType? = null,
  @Embedded
  val sentencingInfo: SentencingInfo? = null,
  @Embedded
  val stopRecord: StopRecord? = null,
  val vulnerabilityDiversity: Boolean? = null,
  val vulnerabilityDiversityDetail: String? = null,
  @Convert(converter = WarrantReferenceNumberJpaConverter::class)
  val warrantReferenceNumber: WarrantReferenceNumber? = null,
) {
  constructor(
    recallId: RecallId,
    nomsNumber: NomsNumber,
    createdByUserId: UserId,
    createdDateTime: OffsetDateTime,
    firstName: FirstName,
    middleNames: MiddleNames?,
    lastName: LastName,
    croNumber: CroNumber,
    dateOfBirth: LocalDate,
    licenceNameCategory: NameFormatCategory = FIRST_LAST,
    lastUpdatedByUserId: UserId = createdByUserId,
    lastUpdatedDateTime: OffsetDateTime = createdDateTime,
    documents: Set<Document> = emptySet(),
    missingDocumentsRecords: Set<MissingDocumentsRecord> = emptySet(),
    lastKnownAddresses: Set<LastKnownAddress> = emptySet(),
    rescindRecords: Set<RescindRecord> = emptySet(),
    recallType: RecallType? = null,
    recallLength: RecallLength? = null,
    lastReleasePrison: PrisonId? = null,
    lastReleaseDate: LocalDate? = null,
    recallEmailReceivedDateTime: OffsetDateTime? = null,
    localPoliceForceId: PoliceForceId? = null,
    inCustodyAtBooking: Boolean? = null,
    inCustodyAtAssessment: Boolean? = null,
    contraband: Boolean? = null,
    contrabandDetail: String? = null,
    vulnerabilityDiversity: Boolean? = null,
    vulnerabilityDiversityDetail: String? = null,
    mappaLevel: MappaLevel? = null,
    sentencingInfo: SentencingInfo? = null,
    bookingNumber: String? = null,
    probationInfo: ProbationInfo? = null,
    licenceConditionsBreached: String? = null,
    reasonsForRecall: Set<ReasonForRecall> = emptySet(),
    reasonsForRecallOtherDetail: String? = null,
    agreeWithRecall: AgreeWithRecall? = null,
    agreeWithRecallDetail: String? = null,
    currentPrison: PrisonId? = null,
    additionalLicenceConditions: Boolean? = null,
    additionalLicenceConditionsDetail: String? = null,
    differentNomsNumber: Boolean? = null,
    differentNomsNumberDetail: String? = null,
    recallNotificationEmailSentDateTime: OffsetDateTime? = null,
    dossierEmailSentDate: LocalDate? = null,
    previousConvictionMainNameCategory: NameFormatCategory? = null,
    hasDossierBeenChecked: Boolean? = null,
    previousConvictionMainName: String? = null,
    assessedByUserId: UserId? = null,
    bookedByUserId: UserId? = null,
    dossierCreatedByUserId: UserId? = null,
    dossierTargetDate: LocalDate? = null,
    assignee: UserId? = null,
    lastKnownAddressOption: LastKnownAddressOption? = null,
    arrestIssues: Boolean? = null,
    arrestIssuesDetail: String? = null,
    warrantReferenceNumber: WarrantReferenceNumber? = null,
    stopRecord: StopRecord? = null,
  ) :
    this(
      recallId.value,
      nomsNumber,
      createdByUserId.value,
      createdDateTime,
      lastUpdatedByUserId.value,
      lastUpdatedDateTime,
      firstName,
      middleNames,
      lastName,
      croNumber,
      dateOfBirth,
      licenceNameCategory,
      documents,
      lastKnownAddresses,
      missingDocumentsRecords,
      reasonsForRecall,
      rescindRecords,
      additionalLicenceConditions,
      additionalLicenceConditionsDetail,
      agreeWithRecall,
      agreeWithRecallDetail,
      arrestIssues,
      arrestIssuesDetail,
      assessedByUserId?.value,
      assignee?.value,
      bookedByUserId?.value,
      bookingNumber,
      contraband,
      contrabandDetail,
      currentPrison,
      differentNomsNumber,
      differentNomsNumberDetail,
      dossierCreatedByUserId?.value,
      dossierEmailSentDate,
      dossierTargetDate,
      hasDossierBeenChecked,
      inCustodyAtBooking,
      inCustodyAtAssessment,
      lastKnownAddressOption,
      lastReleaseDate,
      lastReleasePrison,
      licenceConditionsBreached,
      localPoliceForceId,
      mappaLevel,
      previousConvictionMainName,
      previousConvictionMainNameCategory,
      probationInfo,
      reasonsForRecallOtherDetail,
      recallEmailReceivedDateTime,
      recallLength,
      recallNotificationEmailSentDateTime,
      recallType,
      sentencingInfo,
      stopRecord,
      vulnerabilityDiversity,
      vulnerabilityDiversityDetail,
      warrantReferenceNumber,
    )

  fun recallId() = RecallId(id)
  fun createdByUserId() = createdByUserId.let(::UserId)
  fun assessedByUserId() = assessedByUserId?.let(::UserId)
  fun bookedByUserId() = bookedByUserId?.let(::UserId)
  fun dossierCreatedByUserId() = dossierCreatedByUserId?.let(::UserId)
  fun assignee() = assignee?.let(::UserId)

  fun recallAssessmentDueDateTime(): OffsetDateTime? = recallEmailReceivedDateTime?.plusHours(24)

  fun prisonerNameOnLicense(): FullName =
    prisonerName().let {
      when (licenceNameCategory) {
        FIRST_LAST -> it.firstAndLastName()
        FIRST_MIDDLE_LAST -> it.firstMiddleLast()
        OTHER -> throw IllegalStateException("OTHER licenceNameCategory not supported")
      }
    }

  private fun prisonerName() = PersonName(firstName, middleNames, lastName)

  fun previousConvictionMainName(): String {
    return when (previousConvictionMainNameCategory) {
      FIRST_LAST -> prisonerName().firstAndLastName().value
      FIRST_MIDDLE_LAST -> prisonerName().firstMiddleLast().value
      OTHER -> previousConvictionMainName!!
      else -> throw IllegalStateException("Unexpected or unset previousConvictionMainNameCategory $previousConvictionMainNameCategory")
    }
  }

  fun inCustodyRecall(): Boolean = inCustodyAtAssessment ?: inCustodyAtBooking!!

  fun status(): Status =
    if (stopRecord != null) {
      Status.STOPPED
    } else if (dossierCreatedByUserId != null) {
      Status.DOSSIER_ISSUED
    } else if (assessedByUserId != null) {
      if (inCustodyRecall()) {
        if (assignee != null) {
          Status.DOSSIER_IN_PROGRESS
        } else {
          Status.AWAITING_DOSSIER_CREATION
        }
      } else {
        if (warrantReferenceNumber != null) {
          Status.AWAITING_RETURN_TO_CUSTODY
        } else {
          Status.ASSESSED_NOT_IN_CUSTODY
        }
      }
    } else if (bookedByUserId != null) {
      if (agreeWithRecall == AgreeWithRecall.NO_STOP) {
        Status.STOPPED
      } else if (assignee != null) {
        Status.IN_ASSESSMENT
      } else {
        Status.BOOKED_ON
      }
    } else {
      Status.BEING_BOOKED_ON
    }
}

@Embeddable
data class SentencingInfo(
  val sentenceDate: LocalDate,
  val licenceExpiryDate: LocalDate,
  val sentenceExpiryDate: LocalDate,
  @Convert(converter = CourtIdJpaConverter::class)
  val sentencingCourt: CourtId,
  val indexOffence: String,
  @Embedded
  val sentenceLength: SentenceLength,
  val conditionalReleaseDate: LocalDate? = null
) {
  fun calculateRecallLength(): RecallLength {
    return if (sentenceLength.sentenceDays >= 366 || sentenceLength.sentenceMonths >= 12 || sentenceLength.sentenceYears >= 1) {
      RecallLength.TWENTY_EIGHT_DAYS
    } else
      RecallLength.FOURTEEN_DAYS
  }
}

@Suppress("JpaAttributeMemberSignatureInspection") // Suppressing erroneous Idea error: https://youtrack.jetbrains.com/issue/IDEA-240844
@Embeddable
data class SentenceLength(val sentenceYears: Int, val sentenceMonths: Int, val sentenceDays: Int) {
  override fun toString(): String = "$sentenceYears years $sentenceMonths months $sentenceDays days"
}

@Embeddable
data class ProbationInfo(
  val probationOfficerName: String,
  val probationOfficerPhoneNumber: String,
  val probationOfficerEmail: String,
  @Enumerated(STRING)
  val localDeliveryUnit: LocalDeliveryUnit,
  val authorisingAssistantChiefOfficer: String
)

@Suppress("JpaAttributeMemberSignatureInspection")
@Embeddable
data class StopRecord(
  @Enumerated(STRING)
  val stoppedReason: StoppedReason,
  val stoppedByUserId: UUID,
  val stoppedDateTime: OffsetDateTime
) {
  constructor(stoppedReason: StoppedReason, stoppedByUserId: UserId, stoppedDateTime: OffsetDateTime) :
    this(
      stoppedReason,
      stoppedByUserId.value,
      stoppedDateTime
    )
  fun stoppedByUserId() = UserId(stoppedByUserId)
}
