package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService

@Component
class DossierContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService
) {
  fun createContext(recallId: RecallId): DossierContext {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison!!)
    return DossierContext(recall, currentPrisonName)
  }
}

data class DossierContext(
  val recall: Recall,
  val currentPrisonName: PrisonName,
) {
  fun getReasonsForRecallContext(): ReasonsForRecallContext {
    return ReasonsForRecallContext(
      recall.prisonerNameOnLicense(),
      recall.bookingNumber!!,
      recall.nomsNumber,
      recall.licenceConditionsBreached!!
    )
  }

  fun getTableOfContentsContext(): TableOfContentsContext =
    TableOfContentsContext(
      recall.prisonerNameOnLicense(),
      RecallLengthDescription(recall.recallLength!!),
      currentPrisonName,
      recall.bookingNumber!!
    )

  fun includeWelsh(): Boolean {
    return recall.probationInfo!!.localDeliveryUnit.isInWales
  }
}

data class TableOfContentsItem(val title: String, val pageNumber: Int)
data class TableOfContentsContext(
  val prisonerNameOnLicense: FullName,
  val recallLengthDescription: RecallLengthDescription,
  val currentPrisonName: PrisonName,
  val bookingNumber: String
)

data class ReasonsForRecallContext(
  val prisonerNameOnLicense: FullName,
  val bookingNumber: String,
  val nomsNumber: NomsNumber,
  val licenceConditionsBreached: String
)
