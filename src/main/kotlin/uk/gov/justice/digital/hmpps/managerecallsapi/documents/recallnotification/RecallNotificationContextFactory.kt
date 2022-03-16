package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LastKnownAddressOption
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PoliceForceLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class RecallNotificationContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val userDetailsService: UserDetailsService,
  @Autowired private val courtLookupService: CourtLookupService,
  @Autowired private val policeForceLookupService: PoliceForceLookupService,
  @Autowired private val documentRepository: DocumentRepository,
  @Autowired private val clock: Clock
) {
  fun createContext(recallId: RecallId, currentUserId: UserId): RecallNotificationContext {
    val recall = recallRepository.getByRecallId(recallId)
    val currentUserDetails = userDetailsService.get(currentUserId)
    val currentPrisonName = if (recall.inCustodyRecall()) prisonLookupService.getPrisonName(recall.currentPrison!!) else null
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison!!)
    val sentencingCourtName = courtLookupService.getCourtName(recall.sentencingInfo!!.sentencingCourt)
    val localPoliceForceName = policeForceLookupService.getPoliceForceName(recall.localPoliceForceId!!)
    val originalRecallNotificationCreatedDateTime = documentRepository.findByRecallIdAndCategoryAndVersion(
      recallId.value,
      RECALL_NOTIFICATION,
      1
    )?.createdDateTime
      ?: OffsetDateTime.now(clock)
    return RecallNotificationContext(
      recall,
      currentUserDetails,
      currentPrisonName,
      lastReleasePrisonName,
      sentencingCourtName,
      localPoliceForceName,
      originalRecallNotificationCreatedDateTime
    )
  }
}

data class RecallNotificationContext(
  val recall: Recall,
  val currentUserDetails: UserDetails,
  val currentPrisonName: PrisonName?,
  val lastReleasePrisonName: PrisonName,
  val sentencingCourtName: CourtName,
  val localPoliceForceName: PoliceForceName,
  val originalRecallNotificationCreatedDateTime: OffsetDateTime
) {
  fun getRevocationOrderContext(): RevocationOrderContext {
    return RevocationOrderContext(
      recall.recallId(),
      recall.prisonerNameOnLicence(),
      recall.dateOfBirth,
      recall.bookingNumber!!,
      recall.croNumber,
      originalRecallNotificationCreatedDateTime.toLocalDate(),
      recall.lastReleaseDate!!,
      currentUserDetails.signature,
      currentUserDetails.userId(),
      recall.lastThenFirstName()
    )
  }

  fun getRecallSummaryContext(): RecallSummaryContext {
    return RecallSummaryContext(
      originalRecallNotificationCreatedDateTime.toZonedDateTime().withZoneSameInstant(ZoneId.of("Europe/London")),
      recall.prisonerNameOnLicence(),
      recall.dateOfBirth,
      recall.croNumber,
      currentUserDetails.personName(),
      currentUserDetails.email,
      currentUserDetails.phoneNumber,
      recall.mappaLevel!!,
      recall.sentencingInfo!!.sentenceLength,
      recall.sentencingInfo.indexOffence,
      sentencingCourtName,
      recall.sentencingInfo.sentenceDate,
      recall.sentencingInfo.sentenceExpiryDate,
      recall.probationInfo!!.probationOfficerName,
      recall.probationInfo.probationOfficerPhoneNumber,
      recall.probationInfo.localDeliveryUnit,
      recall.previousConvictionMainName(),
      recall.bookingNumber!!,
      recall.nomsNumber,
      recall.lastReleaseDate!!,
      recall.reasonsForRecall,
      localPoliceForceName,
      recall.contraband!!,
      recall.contrabandDetail,
      recall.vulnerabilityDiversity!!,
      recall.vulnerabilityDiversityDetail,
      currentPrisonName,
      lastReleasePrisonName,
      recall.inCustodyRecall(),
      recall.arrestIssues,
      recall.arrestIssuesDetail,
      lastKnownAddressText(recall.inCustodyRecall(), recall.lastKnownAddressOption, recall.lastKnownAddresses)
    )
  }

  private fun lastKnownAddressText(
    inCustodyRecall: Boolean,
    lastKnownAddressOption: LastKnownAddressOption?,
    lastKnownAddresses: Set<LastKnownAddress>
  ): String? {
    if (inCustodyRecall) return null
    return when (lastKnownAddressOption!!) {
      LastKnownAddressOption.NO_FIXED_ABODE -> lastKnownAddressOption.label
      else -> lastKnownAddresses.sortedBy { it.index }.joinToString("\n") { it.toAddressString() }
    }
  }

  fun getLetterToProbationContext(): LetterToProbationContext =
    LetterToProbationContext(
      originalRecallNotificationCreatedDateTime.toLocalDate(),
      RecallDescription(recall.recallType(), recall.recallLength),
      recall.probationInfo!!.probationOfficerName,
      recall.prisonerNameOnLicence(),
      recall.bookingNumber!!,
      currentPrisonName,
      currentUserDetails.personName(),
      recall.inCustodyRecall(),
      recall.recallType()
    )

  fun getOffenderNotificationContext(): OffenderNotificationContext =
    OffenderNotificationContext(
      recall.prisonerNameOnLicence(),
      recall.bookingNumber!!,
      originalRecallNotificationCreatedDateTime.toLocalDate(),
      buildReasonsForRecall()
    )

  private fun buildReasonsForRecall(): List<String> {
    val reasonsForRecall = recall.reasonsForRecall.filter { it != OTHER }.map { it.label }.sorted()
    return if (reasonsForRecall.size < recall.reasonsForRecall.size) {
      reasonsForRecall + recall.reasonsForRecallOtherDetail!!
    } else {
      reasonsForRecall
    }
  }
}

private fun LastKnownAddress.toAddressString(): String {
  val optionalLine2 = if (line2 == null) "" else "$line2; "
  val optionalPostcode = if (postcode == null) "" else "; $postcode"
  return "$line1; $optionalLine2$town$optionalPostcode"
}

data class OffenderNotificationContext(
  val prisonerNameOnLicence: FullName,
  val bookingNumber: String,
  val licenceRevocationDate: LocalDate,
  val reasonsForRecall: List<String>
)

data class LetterToProbationContext(
  val licenceRevocationDate: LocalDate,
  val recallDescription: RecallDescription?,
  val probationOfficerName: String,
  val prisonerNameOnLicence: FullName,
  val bookingNumber: String,
  val currentPrisonName: PrisonName?,
  val assessedByUserName: PersonName,
  val inCustodyRecall: Boolean,
  val recallType: RecallType
)

data class RecallSummaryContext(
  val originalCreatedDateTime: ZonedDateTime,
  val prisonerNameOnLicence: FullName,
  val dateOfBirth: LocalDate,
  val croNumber: CroNumber,
  val assessedByUserName: PersonName,
  val assessedByUserEmail: Email,
  val assessedByUserPhoneNumber: PhoneNumber,
  val mappaLevel: MappaLevel,
  val lengthOfSentence: SentenceLength,
  val indexOffence: String,
  val sentencingCourt: CourtName,
  val sentenceDate: LocalDate,
  val sentenceExpiryDate: LocalDate,
  val probationOfficerName: String,
  val probationOfficerPhoneNumber: String,
  val localDeliveryUnit: LocalDeliveryUnit,
  val previousConvictionMainName: String,
  val bookingNumber: String,
  val nomsNumber: NomsNumber,
  val lastReleaseDate: LocalDate,
  val reasonsForRecall: Set<ReasonForRecall>,
  val localPoliceForceName: PoliceForceName,
  val contraband: Boolean,
  val contrabandDetail: String?,
  val vulnerabilityDiversity: Boolean,
  val vulnerabilityDiversityDetail: String?,
  val currentPrisonName: PrisonName?,
  val lastReleasePrisonName: PrisonName,
  val inCustodyRecall: Boolean,
  val arrestIssues: Boolean?,
  val arrestIssuesDetail: String?,
  val lastKnownAddress: String?
)

data class RevocationOrderContext(
  val recallId: RecallId,
  val prisonerNameOnLicence: FullName,
  val dateOfBirth: LocalDate,
  val bookingNumber: String,
  val croNumber: CroNumber,
  val licenseRevocationDate: LocalDate,
  val lastReleaseDate: LocalDate,
  val currentUserSignature: String,
  val currentUserId: UserId,
  val lastThenFirstName: String,
) {
  fun fileName(): FileName = FileName("${lastThenFirstName.uppercase()} ${bookingNumber.uppercase()} REVOCATION ORDER.pdf")
}
