package uk.gov.justice.digital.hmpps.managerecallsapi.documents.returnedtocustodylettertoprobation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import java.time.LocalDate

@Component
class ReturnedToCustodyLetterToProbationContextFactory(
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val documentRepository: DocumentRepository,
) {
  fun createContext(recall: Recall, currentUserId: UserId): ReturnedToCustodyLetterToProbationContext {
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison!!)
    val recallDescription = RecallDescription(recall.recallType(), recall.recallLength)
    val originalCreatedDate = documentRepository.findByRecallIdAndCategoryAndVersion(recall.id, DocumentCategory.LETTER_TO_PROBATION, 1)?.createdDateTime?.toLocalDate() ?: LocalDate.now()
    val partBDueDate = recall.partBDueDate
    return ReturnedToCustodyLetterToProbationContext(
      recallDescription,
      recall.bookingNumber!!,
      recall.nomsNumberHeldUnder(),
      recall.differentNomsNumber!!,
      recall.nomsNumber,
      recall.probationInfo!!.probationOfficerName,
      recall.prisonerNameOnLicence(),
      currentPrisonName,
      recall.returnedToCustody!!.returnedToCustodyDateTime.toLocalDate(),
      originalCreatedDate,
      recall.probationInfo.authorisingAssistantChiefOfficer,
      partBDueDate
    )
  }
}

data class ReturnedToCustodyLetterToProbationContext(
  val recallDescription: RecallDescription,
  val bookingNumber: BookingNumber,
  val nomsNumberHeldUnder: NomsNumber,
  val differentNomsNumber: Boolean,
  val originalNomsNumber: NomsNumber,
  val probationOfficerName: FullName,
  val prisonerNameOnLicence: FullName,
  val currentPrisonName: PrisonName,
  val returnedToCustodyDate: LocalDate,
  val originalCreatedDate: LocalDate,
  val authorisingAssistantChiefOfficer: FullName,
  val partBDueDate: LocalDate?,
)
