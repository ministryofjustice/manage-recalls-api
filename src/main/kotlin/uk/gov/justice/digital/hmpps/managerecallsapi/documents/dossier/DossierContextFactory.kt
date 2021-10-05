package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PersonName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.fullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient

@Component
class DossierContextFactory(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient
) {
  fun createContext(recallId: RecallId): DossierContext {
    val recall = recallRepository.getByRecallId(recallId)
    val prisoner = prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber)).block()!!.first()
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison()!!)
    return DossierContext(recall, prisoner, currentPrisonName)
  }
}

data class DossierContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val currentPrisonName: PrisonName,
) {
  fun getReasonsForRecallContext(): ReasonsForRecallContext {
    return ReasonsForRecallContext(
      PersonName(FirstName(prisoner.firstName!!), prisoner.middleNames?.let { MiddleNames(it) }, LastName(prisoner.lastName!!)),
      recall.bookingNumber!!,
      recall.nomsNumber,
      recall.licenceConditionsBreached!!
    )
  }

  fun getTableOfContentsContext(): TableOfContentsContext =
    TableOfContentsContext(
      prisoner.fullName(),
      RecallLengthDescription(recall.recallLength!!),
      currentPrisonName,
      recall.bookingNumber!!
    )
}

data class TableOfContentsItem(val title: String, val pageNumber: Int)
data class TableOfContentsContext(
  val fullName: PersonName,
  val recallLengthDescription: RecallLengthDescription,
  val currentPrisonName: PrisonName,
  val bookingNumber: String
)

data class ReasonsForRecallContext(
  val personName: PersonName,
  val bookingNumber: String,
  val nomsNumber: NomsNumber,
  val licenceConditionsBreached: String
)
