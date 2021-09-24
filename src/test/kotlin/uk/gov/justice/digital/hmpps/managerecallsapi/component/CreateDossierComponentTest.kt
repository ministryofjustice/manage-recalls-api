package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.hasNumberOfPages
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate
import java.util.Base64

class CreateDossierComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedMergedPdf = ClassPathResource("/document/recall-notification.pdf").file.readBytes()

  @Test
  fun `can generate the dossier sending the correct documents to gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)

    val revocationOrderFile = ClassPathResource("/document/revocation-order.pdf").file

    gotenbergMockServer.stubMergePdfs(
      expectedMergedPdf,
      ClassPathResource("/document/licence.pdf").file.readText(),
      ClassPathResource("/document/part_a.pdf").file.readText(),
      revocationOrderFile.readText(),
    )

    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(LICENCE, base64EncodedFile("/document/licence.pdf"), null)
    )
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(PART_A_RECALL_REPORT, base64EncodedFile("/document/part_a.pdf"), null)
    )
    authenticatedClient.uploadRecallDocument(
      recall.recallId,
      AddDocumentRequest(REVOCATION_ORDER, base64EncodedFile("/document/revocation-order.pdf"), null)
    )

    val dossier = authenticatedClient.getDossier(recall.recallId)

    assertThat(dossier, hasNumberOfPages(equalTo(3)))
  }

  private fun base64EncodedFile(fileName: String) =
    Base64.getEncoder().encodeToString(ClassPathResource(fileName).file.readBytes())

  private fun expectAPrisonerWillBeFoundFor(nomsNumber: NomsNumber, firstName: String) {
    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber.value,
          firstName = firstName,
          lastName = "Oskina",
          dateOfBirth = LocalDate.of(2000, 1, 31),
          bookNumber = "Book Num 123",
          croNumber = "CRO Num/456"
        )
      )
    )
  }
}
