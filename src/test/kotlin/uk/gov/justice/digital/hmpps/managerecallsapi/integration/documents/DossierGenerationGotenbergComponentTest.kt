package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import java.time.LocalDate

class DossierGenerationGotenbergComponentTest : GotenbergComponentTestBase() {

  @Test
  fun `can generate a dossier using gotenberg for an English LDU and English current Prison`() {
    generateDossierUsingGotenberg(LocalDeliveryUnit.PS_HOUNSLOW, 11, PrisonId("MWI"))
  }

  @Test
  fun `can generate a dossier using gotenberg for a Welsh LDU and English current Prison`() {
    generateDossierUsingGotenberg(LocalDeliveryUnit.PS_NORTH_WALES, 15, PrisonId("MWI"))
  }

  @Test
  fun `can generate a dossier using gotenberg for an English LDU and Welsh current Prison`() {
    generateDossierUsingGotenberg(LocalDeliveryUnit.PS_HOUNSLOW, 15, PrisonId("CFI"))
  }

  private fun generateDossierUsingGotenberg(
    localDeliveryUnit: LocalDeliveryUnit,
    expectedPageCount: Int,
    currentPrisonId: PrisonId
  ) {
    val nomsNumber = NomsNumber("123456")
    val prisonerFirstName = "Natalia"
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)

    val recall =
      authenticatedClient.bookRecall(
        BookRecallRequest(
          nomsNumber,
          FirstName("Barrie"),
          null,
          LastName("Badger"),
          CroNumber("1234/56A"),
          LocalDate.now()
        )
      )
    updateRecallWithRequiredInformationForTheDossier(
      recall.recallId,
      localDeliveryUnit = localDeliveryUnit,
      currentPrisonId = currentPrisonId
    )
    authenticatedClient.generateDocument(recall.recallId, RECALL_NOTIFICATION)
    expectNoVirusesWillBeFound()
    uploadLicenceFor(recall)
    uploadPartAFor(recall)

    val generateResponse = authenticatedClient.generateDocument(recall.recallId, DOSSIER)
    val dossier = authenticatedClient.getDocument(recall.recallId, generateResponse.documentId)
    // writeBase64EncodedStringToFile("dossier-$expectedPageCount-pages.pdf", dossier.content)
    assertThat(Pdf(dossier.content), hasNumberOfPages(equalTo(expectedPageCount)))
  }
}
