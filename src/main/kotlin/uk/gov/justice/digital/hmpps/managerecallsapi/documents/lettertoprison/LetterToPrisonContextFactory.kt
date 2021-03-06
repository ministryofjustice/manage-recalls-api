package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import java.time.LocalDate

@Component
class LetterToPrisonContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val documentRepository: DocumentRepository,
) {
  fun createContext(recallId: RecallId, currentUserId: UserId): LetterToPrisonContext {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison!!)
    val lastReleasePrisonName = prisonLookupService.getPrisonName(recall.lastReleasePrison!!)
    val recallDescription = RecallDescription(recall.recallType(), recall.recallLength)
    val originalCreatedDate = documentRepository.findByRecallIdAndCategoryAndVersion(
      recallId.value,
      LETTER_TO_PRISON,
      1
    )?.createdDateTime?.toLocalDate() ?: LocalDate.now()
    return LetterToPrisonContext(
      recall.prisonerNameOnLicence(),
      currentPrisonName,
      lastReleasePrisonName,
      recallDescription,
      recall.bookingNumber!!,
      recall.lastReleaseDate!!,
      originalCreatedDate,
      recall.nomsNumberHeldUnder(),
      recall.differentNomsNumber!!,
      recall.nomsNumber,
      recall.additionalLicenceConditions!!,
      recall.additionalLicenceConditionsDetail,
      recall.contraband!!,
      recall.contrabandDetail,
      recall.vulnerabilityDiversity!!,
      recall.vulnerabilityDiversityDetail,
      recall.mappaLevel!!
    )
  }
}

data class LetterToPrisonContext(
  val prisonerNameOnLicence: FullName,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName,
  val recallDescription: RecallDescription,
  val bookingNumber: BookingNumber,
  val lastReleaseDate: LocalDate,
  val originalCreatedDate: LocalDate,
  val nomsNumberHeldUnder: NomsNumber,
  val differentNomsNumber: Boolean,
  val originalNomsNumber: NomsNumber,
  val hasAdditionalLicenceConditions: Boolean,
  val additionalLicenceConditionsDetail: String?,
  val hasContraband: Boolean,
  val contrabandDetail: String?,
  val hasVulnerabilities: Boolean,
  val vulnerabilityDetail: String?,
  val mappaLevel: MappaLevel
) {
  fun getCustodyContext(): LetterToPrisonCustodyOfficeContext =
    LetterToPrisonCustodyOfficeContext(
      prisonerNameOnLicence,
      currentPrisonName,
      recallDescription,
      bookingNumber,
      originalCreatedDate,
      nomsNumberHeldUnder,
      differentNomsNumber,
      originalNomsNumber,
      hasAdditionalLicenceConditions,
      additionalLicenceConditionsDetail,
      hasContraband,
      contrabandDetail,
      hasVulnerabilities,
      vulnerabilityDetail,
      mappaLevel
    )

  fun getGovernorContext(): LetterToPrisonGovernorContext =
    LetterToPrisonGovernorContext(
      prisonerNameOnLicence,
      currentPrisonName,
      lastReleasePrisonName,
      recallDescription,
      bookingNumber,
      lastReleaseDate,
      originalCreatedDate
    )

  fun getConfirmationContext(): LetterToPrisonConfirmationContext =
    LetterToPrisonConfirmationContext(prisonerNameOnLicence, recallDescription, bookingNumber)

  fun getStandardPartsContext(): LetterToPrisonStandardPartsContext =
    LetterToPrisonStandardPartsContext(prisonerNameOnLicence, bookingNumber, originalCreatedDate, currentPrisonName)
}

data class LetterToPrisonGovernorContext(
  val prisonerNameOnLicence: FullName,
  val currentPrisonName: PrisonName,
  val lastReleasePrisonName: PrisonName,
  val recallDescription: RecallDescription,
  val bookingNumber: BookingNumber,
  val lastReleaseDate: LocalDate,
  val originalCreatedDate: LocalDate,
)

data class LetterToPrisonConfirmationContext(
  val prisonerNameOnLicence: FullName,
  val recallDescription: RecallDescription,
  val bookingNumber: BookingNumber,
)

data class LetterToPrisonCustodyOfficeContext(
  val prisonerNameOnLicence: FullName,
  val currentPrisonName: PrisonName,
  val recallDescription: RecallDescription,
  val bookingNumber: BookingNumber,
  val originalCreatedDate: LocalDate,
  val nomsNumberHeldUnder: NomsNumber,
  val differentNomsNumber: Boolean,
  val originalNomsNumber: NomsNumber,
  val hasAdditionalLicenceConditions: Boolean,
  val additionalLicenceConditionsDetail: String?,
  val hasContraband: Boolean,
  val contrabandDetail: String?,
  val hasVulnerabilities: Boolean,
  val vulnerabilityDetail: String?,
  val mappaLevel: MappaLevel
)

data class LetterToPrisonStandardPartsContext(
  val prisonerNameOnLicence: FullName,
  val bookingNumber: BookingNumber,
  val originalCreatedDate: LocalDate,
  val currentPrisonName: PrisonName
)
