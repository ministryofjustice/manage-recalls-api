package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class RecallNotificationContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
  @Autowired private val userDetailsService: UserDetailsService,
  @Autowired private val courtLookupService: CourtLookupService,
) {
  fun createContext(recallId: RecallId, userId: UserId): RecallNotificationContext {
    val recall = recallRepository.getByRecallId(recallId)
    val prisoner = prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)).block()!!.first()
    val userDetails = userDetailsService.get(userId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison!!)
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison!!)
    val sentencingCourtName = courtLookupService.getCourtName(recall.sentencingInfo!!.sentencingCourt)
    return RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName, sentencingCourtName)
  }
}

data class RecallNotificationContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val assessedByUserDetails: UserDetails,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName,
  val sentencingCourtName: CourtName,
  private val clock: Clock = Clock.systemUTC()
) {
  fun getRevocationOrderContext(): RevocationOrderContext {
    return RevocationOrderContext(
      recall.recallId(),
      recall.prisonerNameOnLicense(),
      prisoner.dateOfBirth!!,
      recall.bookingNumber!!,
      prisoner.croNumber,
      LocalDate.now(clock),
      recall.lastReleaseDate!!,
      assessedByUserDetails.signature
    )
  }

  fun getRecallSummaryContext(): RecallSummaryContext {
    return RecallSummaryContext(
      ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London")),
      recall.prisonerNameOnLicense(),
      prisoner.dateOfBirth!!,
      prisoner.croNumber,
      assessedByUserDetails.personName(),
      assessedByUserDetails.email,
      assessedByUserDetails.phoneNumber,
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
      recall.localPoliceForce!!,
      recall.contraband!!,
      recall.contrabandDetail,
      recall.vulnerabilityDiversity!!,
      recall.vulnerabilityDiversityDetail,
      currentPrisonName,
      lastReleasePrisonName
    )
  }

  fun getLetterToProbationContext(): LetterToProbationContext =
    LetterToProbationContext(
      LocalDate.now(clock),
      RecallLengthDescription(recall.recallLength!!),
      recall.probationInfo!!.probationOfficerName,
      recall.prisonerNameOnLicense(),
      recall.bookingNumber!!,
      currentPrisonName,
      assessedByUserDetails.personName()
    )
}

data class LetterToProbationContext(
  val licenceRevocationDate: LocalDate,
  val recallLengthDescription: RecallLengthDescription,
  val probationOfficerName: String,
  val prisonerNameOnLicense: FullName,
  val bookingNumber: String,
  val currentPrisonName: PrisonName,
  val assessedByUserName: PersonName
)

data class RecallSummaryContext(
  val createdDateTime: ZonedDateTime,
  val prisonerNameOnLicense: FullName,
  val dateOfBirth: LocalDate,
  val croNumber: String?, // TODO:  Can this really ever be null?  Breaks in dev because we have test prisoners without a croNumber
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
  val localPoliceForce: String,
  val contraband: Boolean,
  val contrabandDetail: String?,
  val vulnerabilityDiversity: Boolean,
  val vulnerabilityDiversityDetail: String?,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName
)

data class RevocationOrderContext(
  val recallId: RecallId,
  val prisonerNameOnLicense: FullName,
  val dateOfBirth: LocalDate,
  val bookingNumber: String,
  val croNumber: String?,
  val licenseRevocationDate: LocalDate,
  val lastReleaseDate: LocalDate,
  val assessedByUserSignature: String
)
