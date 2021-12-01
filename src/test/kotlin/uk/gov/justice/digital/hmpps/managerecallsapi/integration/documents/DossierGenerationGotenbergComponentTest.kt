package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages

class DossierGenerationGotenbergComponentTest : GotenbergComponentTestBase() {

  @Test
  fun `can generate a dossier using gotenberg`() {
    val nomsNumber = NomsNumber("123456")
    val prisonerFirstName = "Natalia"
    expectAPrisonerWillBeFoundFor(nomsNumber, prisonerFirstName)

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber, FirstName("Barrie"), null, LastName("Badger")))
    updateRecallWithRequiredInformationForTheDossier(recall.recallId)
    authenticatedClient.getRecallNotification(recall.recallId)
    expectNoVirusesWillBeFound()
    uploadLicenceFor(recall)
    uploadPartAFor(recall)

    val dossier = authenticatedClient.getDossier(recall.recallId)
    // writeBase64EncodedStringToFile("dossier.pdf", dossier.content)
    assertThat(dossier, hasNumberOfPages(equalTo(11)))
  }
}
