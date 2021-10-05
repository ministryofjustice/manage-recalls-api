package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.ReasonForRecall
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.fullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
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
  @Autowired private val userDetailsService: UserDetailsService
) {
  fun createContext(recallId: RecallId, userId: UserId): RecallNotificationContext {
    val recall = recallRepository.getByRecallId(recallId)
    val prisoner = prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)).block()!!.first()
    val userDetails = userDetailsService.get(userId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison()!!)
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison()!!)
    return RecallNotificationContext(recall, prisoner, userDetails, currentPrisonName, lastReleasePrisonName)
  }
}

data class RecallNotificationContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val assessedByUserDetails: UserDetails,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName,
  private val clock: Clock = Clock.systemUTC()
) {
  fun getRevocationOrderContext(): RevocationOrderContext {
    return RevocationOrderContext(
      recall.recallId(),
      PersonName(FirstName(prisoner.firstName!!), prisoner.middleNames?.let { MiddleNames(it) }, LastName(prisoner.lastName!!)),
      prisoner.dateOfBirth!!,
      recall.bookingNumber!!,
      prisoner.croNumber,
      LocalDate.now(clock),
      recall.lastReleaseDate!!,
      assessedByUserDetails.signature
    )
  }

  fun getRecallSummaryContext(): RecallSummaryContext {
    val prisonerName = PersonName(FirstName(prisoner.firstName!!), prisoner.middleNames?.let { MiddleNames(it) }, LastName(prisoner.lastName!!))
    return RecallSummaryContext(
      ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London")),
      prisonerName,
      prisoner.dateOfBirth!!,
      prisoner.croNumber,
      PersonName(assessedByUserDetails.firstName, null, assessedByUserDetails.lastName),
      assessedByUserDetails.email,
      assessedByUserDetails.phoneNumber,
      recall.mappaLevel!!,
      recall.sentencingInfo!!.sentenceLength,
      recall.sentencingInfo.indexOffence,
      recall.sentencingInfo.sentencingCourt,
      recall.sentencingInfo.sentenceDate,
      recall.sentencingInfo.sentenceExpiryDate,
      recall.probationInfo!!.probationOfficerName,
      recall.probationInfo.probationOfficerPhoneNumber,
      recall.probationInfo.localDeliveryUnit,
      if (recall.hasOtherPreviousConvictionMainName == true) recall.previousConvictionMainName!! else prisonerName.firstAndLastName(),
      recall.bookingNumber!!,
      recall.nomsNumber,
      recall.lastReleaseDate!!,
      recall.reasonsForRecall,
      recall.localPoliceForce!!,
      recall.vulnerabilityDiversityDetail,
      recall.contrabandDetail,
      currentPrisonName,
      lastReleasePrisonName
    )
  }

  fun getLetterToProbationContext(): LetterToProbationContext =
    LetterToProbationContext(
      LocalDate.now(clock),
      RecallLengthDescription(recall.recallLength!!),
      recall.probationInfo!!.probationOfficerName,
      prisoner.fullName(),
      recall.bookingNumber!!,
      currentPrisonName,
      PersonName(assessedByUserDetails.firstName, null, assessedByUserDetails.lastName)
    )
}

data class LetterToProbationContext(
  val licenceRevocationDate: LocalDate,
  val recallLengthDescription: RecallLengthDescription,
  val probationOfficerName: String,
  val offenderName: PersonName,
  val bookingNumber: String,
  val currentPrisonName: PrisonName,
  val assessedByUserName: PersonName
)

data class RecallSummaryContext(
  val createdDateTime: ZonedDateTime,
  val personName: PersonName,
  val dateOfBirth: LocalDate,
  val croNumber: String?, // TODO:  Can this really ever be null?  Breaks in dev because we have test prisoners without a croNumber
  val assessedByUserName: PersonName,
  val assessedByUserEmail: Email,
  val assessedByUserPhoneNumber: PhoneNumber,
  val mappaLevel: MappaLevel,
  val lengthOfSentence: SentenceLength,
  val indexOffence: String,
  val sentencingCourt: String,
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
  val vulnerabilityDiversityDetail: String?,
  val contrabandDetail: String?,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName
)

data class RevocationOrderContext(
  val recallId: RecallId,
  val personName: PersonName,
  val dateOfBirth: LocalDate,
  val bookingNumber: String,
  val croNumber: String?,
  val licenseRevocationDate: LocalDate,
  val lastReleaseDate: LocalDate,
  val assessedByUserSignature: String
)
