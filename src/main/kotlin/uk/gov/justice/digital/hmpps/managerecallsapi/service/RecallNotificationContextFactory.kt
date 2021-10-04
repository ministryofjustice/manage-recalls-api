package uk.gov.justice.digital.hmpps.managerecallsapi.service

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
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison!!)
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison!!)
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
  init {
    /*
     validate required fields?
     e.g.
     prisoner.firstName != null
     prisoner.lastName != null
     etc
     */
  }

  fun getRevocationOrderContext(): RevocationOrderContext {
    return RevocationOrderContext(
      recall.recallId(),
      FirstAndMiddleNames(FirstName(prisoner.firstName!!), prisoner.middleNames?.let { MiddleNames(it) }),
      LastName(prisoner.lastName!!),
      prisoner.dateOfBirth!!,
      recall.bookingNumber!!,
      prisoner.croNumber!!,
      LocalDate.now(clock),
      recall.lastReleaseDate!!,
      assessedByUserDetails.signature
    )
  }

  fun getRecallSummaryContext(): RecallSummaryContext =
    RecallSummaryContext(
      ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London")),
      FirstAndMiddleNames(FirstName(prisoner.firstName!!), prisoner.middleNames?.let { MiddleNames(it) }),
      LastName(prisoner.lastName!!),
      prisoner.dateOfBirth!!,
      prisoner.croNumber!!,
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
      recall.previousConvictionMainName!!,
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

data class RecallSummaryContext(
  val createdDateTime: ZonedDateTime,
  val firstAndMiddleNames: FirstAndMiddleNames,
  val lastName: LastName,
  val dateOfBirth: LocalDate,
  val croNumber: String,
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
  val firstAndMiddleNames: FirstAndMiddleNames,
  val lastName: LastName,
  val dateOfBirth: LocalDate,
  val bookingNumber: String,
  val croNumber: String,
  val today: LocalDate,
  val lastReleaseDate: LocalDate,
  val assessedByUserSignature: String
)
