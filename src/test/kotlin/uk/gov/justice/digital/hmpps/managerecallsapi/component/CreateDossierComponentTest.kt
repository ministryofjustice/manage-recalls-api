package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Api
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.UpdateRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.base64EncodedFileContents
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.RecallInformationLeaflet
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.readText
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate

class CreateDossierComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedMergedPdf = ClassPathResource("/document/recall-notification.pdf").file.readBytes()

  @Test
  fun `can generate the dossier sending the correct documents to gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)

    val revocationOrderFile = ClassPathResource("/document/revocation-order.pdf").file
    val tableOfContentsFile = ClassPathResource("/document/table-of-contents.pdf").file
    val reasonsForRecallContentsFile = ClassPathResource("/document/reasons-for-recall.pdf").file // TODO use rep. pdf

    gotenbergMockServer.stubGenerateTableOfContents(tableOfContentsFile.readBytes())
    gotenbergMockServer.stubGenerateReasonsForRecall(reasonsForRecallContentsFile.readBytes())

    gotenbergMockServer.stubMergePdfs(
      expectedMergedPdf,
      tableOfContentsFile.readText(),
      RecallInformationLeaflet.readText(),
      ClassPathResource("/document/licence.pdf").file.readText(),
      ClassPathResource("/document/part_a.pdf").file.readText(),
      revocationOrderFile.readText(),
      reasonsForRecallContentsFile.readText()
    )

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    authenticatedClient.updateRecall(
      recall.recallId,
      UpdateRecallRequest(
        currentPrison = PrisonId("MWI"),
        lastReleaseDate = LocalDate.of(2021, 9, 2),
        sentenceDate = LocalDate.of(2012, 5, 17),
        licenceExpiryDate = LocalDate.of(2025, 12, 25),
        sentenceExpiryDate = LocalDate.of(2021, 1, 12),
        sentenceLength = Api.SentenceLength(10, 1, 5),
        sentencingCourt = "Badger court",
        indexOffence = "Badgering",
        bookingNumber = "booking number",
        licenceConditionsBreached = "he was a very naughty boy"
      )
    )
    expectNoVirusesWillBeFound()

    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(LICENCE, base64EncodedFileContents("/document/licence.pdf"))
    )
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(PART_A_RECALL_REPORT, base64EncodedFileContents("/document/part_a.pdf"))
    )
    // TODO:  This shouldn't be allowed by the API, temporary way of setting up the revocation order for this test to pass
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(REVOCATION_ORDER, base64EncodedFileContents("/document/revocation-order.pdf"))
    )

    val dossier = authenticatedClient.getDossier(recall.recallId)

    assertThat(dossier, hasNumberOfPages(equalTo(3)))
  }

  private fun expectAPrisonerWillBeFoundFor(nomsNumber: NomsNumber, firstName: String) {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(prisonerNumber = nomsNumber.value, firstName = firstName, lastName = "Badger")
      )
    )
  }
}
