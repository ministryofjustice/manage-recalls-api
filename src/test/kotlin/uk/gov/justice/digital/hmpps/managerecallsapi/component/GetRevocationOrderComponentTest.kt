package uk.gov.justice.digital.hmpps.managerecallsapi.component

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.BookRecallRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import java.time.LocalDate
import java.util.Base64

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

    val response = authenticatedClient.getRevocationOrder(recallId)

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }

  @Test
  fun `get revocation order downloads the document from S3 if it already exists`() {
    val recall = authenticatedClient.bookRecall(BookRecallRequest(nomsNumber))

    expectAPrisonerWillBeFoundFor(nomsNumber, firstName)
    expectAPdfWillBeGenerated(expectedPdf, firstName)

    authenticatedClient.getRevocationOrder(recall.recallId)

    prisonerOffenderSearch.resetAll()
    gotenbergMockServer.resetAll()

    val response = authenticatedClient.getRevocationOrder(recall.recallId)

    assertThat(response.content, equalTo(expectedBase64Pdf))
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
