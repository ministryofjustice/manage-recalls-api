package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate
import java.util.Base64

class CreateDossierComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedPdfString = "Expected Generated PDF"
  private val expectedPdf = expectedPdfString.toByteArray()
  private val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

  @Test
  fun `can generate the dossier sending the correct documents to gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)
    expectTheRevocationOrderWillBeGenerated(expectedPdf, firstName)
    gotenbergMockServer.stubMergePdfs(
      expectedPdf,
      "3-license.pdf" to ClassPathResource("/document/licence.pdf").file.readText(),
      "6-partA_RecallReport.pdf" to ClassPathResource("/document/part_a.pdf").file.readText(),
      "9-revocationOrder.pdf" to expectedPdfString
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

    val dossier = authenticatedClient.getDossier(recall.recallId)

    assertThat(dossier.content, equalTo(expectedBase64Pdf))
  }

  private fun base64EncodedFile(fileName: String) =
    Base64.getEncoder().encodeToString(ClassPathResource(fileName).file.readBytes())

  private fun expectTheRevocationOrderWillBeGenerated(expectedPdf: ByteArray, firstName: String) {
    gotenbergMockServer.stubPdfGeneration(expectedPdf, firstName)
  }

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
