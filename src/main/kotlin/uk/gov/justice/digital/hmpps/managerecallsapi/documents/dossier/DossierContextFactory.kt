package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
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
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val documentRepository: DocumentRepository,
) {
  fun createContext(recallId: RecallId): DossierContext {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonId = recall.currentPrison!!
    val currentPrisonName = prisonLookupService.getPrisonName(currentPrisonId)
    val currentPrisonIsWelsh = prisonLookupService.isWelsh(currentPrisonId)
    val version = (documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, DOSSIER)?.version ?: 0) + 1
    return DossierContext(recall, currentPrisonName, currentPrisonIsWelsh, version)
  }
}

data class DossierContext(
  val recall: Recall,
  val currentPrisonName: PrisonName,
  val currentPrisonIsWelsh: Boolean,
  val version: Int
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
      recall.bookingNumber!!,
      version
    )

  fun includeWelsh(): Boolean {
    return currentPrisonIsWelsh || recall.probationInfo!!.localDeliveryUnit.isInWales
  }
}

data class TableOfContentsItem(val title: String, val pageNumber: Int)
data class TableOfContentsContext(
  val prisonerNameOnLicense: FullName,
  val recallLengthDescription: RecallLengthDescription,
  val currentPrisonName: PrisonName,
  val bookingNumber: String,
  val newVersion: Int
)

data class ReasonsForRecallContext(
  val prisonerNameOnLicense: FullName,
  val bookingNumber: String,
  val nomsNumber: NomsNumber,
  val licenceConditionsBreached: String
)
