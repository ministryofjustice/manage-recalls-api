package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.lowagie.text.pdf.PdfReader
import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.AddDocumentRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
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
  private val expectedMergedPdf = ClassPathResource("/document/recall-notification.pdf").file.readBytes()

  @Test
  fun `can generate the dossier sending the correct documents to gotenberg`() {
    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)

    val revocationOrderFile = ClassPathResource("/document/revocation-order.pdf").file
    gotenbergMockServer.stubPdfGeneration(revocationOrderFile.readBytes(), firstName, "revocation-order-logo")
    gotenbergMockServer.stubMergePdfs(
      expectedMergedPdf,
      "3-license.pdf" to ClassPathResource("/document/licence.pdf").file.readText(),
      "6-partA_RecallReport.pdf" to ClassPathResource("/document/part_a.pdf").file.readText(),
      "9-revocationOrder.pdf" to revocationOrderFile.readText(),
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

//    File("test-results.pdf").writeBytes(Base64.getDecoder().decode(dossier.content))
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

fun hasNumberOfPages(numberOfPagesMatcher: Matcher<Int>): Matcher<Pdf> =
  object : Matcher<Pdf> {
    override val description: String = numberOfPagesMatcher.description

    override fun invoke(actual: Pdf): MatchResult =
      PdfReader(Base64.getDecoder().decode(actual.content.toByteArray())).use { pdfReader ->
        numberOfPagesMatcher(pdfReader.numberOfPages)
      }
  }
