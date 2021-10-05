package uk.gov.justice.digital.hmpps.managerecallsapi.db

import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AgreeWithRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
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
  @Id
  val id: UUID,
  @Column(nullable = false)
  @Convert(converter = NomsNumberJpaConverter::class)
  val nomsNumber: NomsNumber,
  @OneToMany(cascade = [ALL])
  @JoinColumn(name = "recall_id")
  val documents: Set<RecallDocument> = emptySet(),
  @Enumerated(STRING)
  val recallType: RecallType? = null,
  @Enumerated(STRING)
  val recallLength: RecallLength? = null,
  val lastReleasePrison: String? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForce: String? = null,
  val contrabandDetail: String? = null,
  val vulnerabilityDiversityDetail: String? = null,
  @Enumerated(STRING)
  val mappaLevel: MappaLevel? = null,
  val sentencingInfo: SentencingInfo? = null,
  val bookingNumber: String? = null,
  val probationInfo: ProbationInfo? = null,
  val licenceConditionsBreached: String? = null,
  @ElementCollection(targetClass = ReasonForRecall::class)
  @JoinTable(name = "recall_reason", joinColumns = [JoinColumn(name = "recall_id")])
  @Column(name = "reason_for_recall", nullable = false)
  @Enumerated(STRING)
  val reasonsForRecall: Set<ReasonForRecall> = emptySet(),
  val reasonsForRecallOtherDetail: String? = null,
  @Enumerated(STRING)
  val agreeWithRecall: AgreeWithRecall? = null,
  val agreeWithRecallDetail: String? = null,
  val currentPrison: String? = null,
  val additionalLicenceConditions: Boolean? = null,
  val additionalLicenceConditionsDetail: String? = null,
  val differentNomsNumber: Boolean? = null,
  val differentNomsNumberDetail: String? = null,
  val recallNotificationEmailSentDateTime: OffsetDateTime? = null,
  val dossierEmailSentDate: LocalDate? = null,
  val hasOtherPreviousConvictionMainName: Boolean? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val previousConvictionMainName: String? = null,
  // MD: ideally this would be UserId, but hibernate/postgres does not make this easy :-(
  val assessedByUserId: UUID? = null,
  val bookedByUserId: UUID? = null,
  val dossierCreatedByUserId: UUID? = null
) {
  constructor(
    recallId: RecallId,
    nomsNumber: NomsNumber,
    documents: Set<RecallDocument> = emptySet(),
    recallType: RecallType? = null,
    recallLength: RecallLength? = null,
    lastReleasePrison: PrisonId? = null,
    lastReleaseDate: LocalDate? = null,
    recallEmailReceivedDateTime: OffsetDateTime? = null,
    localPoliceForce: String? = null,
    contrabandDetail: String? = null,
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
    hasOtherPreviousConvictionMainName: Boolean? = null,
    hasDossierBeenChecked: Boolean? = null,
    previousConvictionMainName: String? = null,
    assessedByUserId: UserId? = null,
    bookedByUserId: UserId? = null,
    dossierCreatedByUserId: UserId? = null
  ) :
    this(
      recallId.value,
      nomsNumber,
      documents,
      recallType,
      recallLength,
      lastReleasePrison?.value,
      lastReleaseDate,
      recallEmailReceivedDateTime,
      localPoliceForce,
      contrabandDetail,
      vulnerabilityDiversityDetail,
      mappaLevel,
      sentencingInfo,
      bookingNumber,
      probationInfo,
      licenceConditionsBreached,
      reasonsForRecall,
      reasonsForRecallOtherDetail,
      agreeWithRecall,
      agreeWithRecallDetail,
      currentPrison?.value,
      additionalLicenceConditions,
      additionalLicenceConditionsDetail,
      differentNomsNumber,
      differentNomsNumberDetail,
      recallNotificationEmailSentDateTime,
      dossierEmailSentDate,
      hasOtherPreviousConvictionMainName,
      hasDossierBeenChecked,
      previousConvictionMainName,
      assessedByUserId?.value,
      bookedByUserId?.value,
      dossierCreatedByUserId?.value
    )

  fun recallId() = RecallId(id)
  fun assessedByUserId() = assessedByUserId?.let { UserId(it) }
  fun bookedByUserId() = bookedByUserId?.let { UserId(it) }
  fun dossierCreatedByUserId() = dossierCreatedByUserId?.let { UserId(it) }
  fun lastReleasePrison() = lastReleasePrison?.let { PrisonId(it) }
  fun currentPrison() = currentPrison?.let { PrisonId(it) }
}

@Embeddable
data class SentencingInfo(
  val sentenceDate: LocalDate,
  val licenceExpiryDate: LocalDate,
  val sentenceExpiryDate: LocalDate,
  val sentencingCourt: String,
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
