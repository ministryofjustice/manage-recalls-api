package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

// TODO: MD  Use localstack instead of mocking S3Service
class GetRevocationOrderComponentTest : ComponentTestBase() {

  private val nomsNumber = NomsNumber("123456")
  private val firstName = "Natalia"
  private val expectedPdf = "Expected Generated PDF".toByteArray()
  private val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

  @Test
  fun `get revocation order generates a Pdf and uploads to S3 if it does not already exist`() {
    val recallId = ::RecallId.random()
    recallRepository.save(Recall(recallId, nomsNumber))

    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)
    expectAPdfWillBeGenerated(expectedPdf, firstName)
    expectTheRevocationOrderWillBeUploadedToS3()

    val response = authenticatedClient.getRevocationOrder(recallId)

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }

  @Test
  fun `get revocation order downloads the document from S3 if it already exists`() {
    val recallId = ::RecallId.random()
    val revocationOrderId = UUID.randomUUID()
    recallRepository.save(Recall(recallId, nomsNumber, revocationOrderId = revocationOrderId))

    expectTheRevocationOrderWillBeDownloadedFromS3(revocationOrderId, expectedPdf)

    val response = authenticatedClient.getRevocationOrder(recallId)

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }

  private fun expectTheRevocationOrderWillBeDownloadedFromS3(revocationOrderId: UUID, expectedPdf: ByteArray) {
    every { s3Service.downloadFile(revocationOrderId) } returns expectedPdf
  }

  private fun expectTheRevocationOrderWillBeUploadedToS3() {
    every { s3Service.uploadFile(any()) } returns UUID.randomUUID()
  }

  private fun expectAPdfWillBeGenerated(expectedPdf: ByteArray, firstName: String) {
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
