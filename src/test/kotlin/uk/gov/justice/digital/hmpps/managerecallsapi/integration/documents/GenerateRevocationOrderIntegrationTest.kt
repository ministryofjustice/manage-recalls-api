package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RevocationOrderRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerSearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.storage.S3BulkResponseEntity
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

class GenerateRevocationOrderIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var gotenbergMockServer: GotenbergMockServer

  @BeforeAll
  fun startGotenberg() {
    gotenbergMockServer.start()
  }

  @AfterAll
  fun stopGotenberg() {
    gotenbergMockServer.stop()
  }

  @Test
  fun `should respond with 401 if user does not have the MANAGE_RECALLS role`() {
    val invalidUserJwt = testJwt("ROLE_UNKNOWN")
    sendAuthenticatedPostRequestWithBody(
      "/generate-revocation-order",
      RevocationOrderRequest("A Noms Number"),
      invalidUserJwt
    )
      .expectStatus().isUnauthorized
  }

  @Test
  fun `generate revocation order endpoint should return generated pdf`() {
    every { s3Service.uploadFile(any(), any(), any()) } returns S3BulkResponseEntity(
      "bucket-name",
      UUID.randomUUID(),
      "myFile.pdf",
      true,
      200
    )

    val firstName = "Natalia"
    val nomsNumber = "A Noms Number"
    val expectedPdf = "Expected Generated PDF".toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)
    gotenbergMockServer.stubPdfGeneration(expectedPdf, firstName)

    prisonerOffenderSearch.prisonerSearchRespondsWith(
      PrisonerSearchRequest(nomsNumber),
      listOf(
        Prisoner(
          prisonerNumber = nomsNumber,
          firstName = firstName,
          lastName = "Oskina",
          dateOfBirth = LocalDate.of(2000, 1, 31),
          bookNumber = "Book Num 123",
          croNumber = "CRO Num/456"
        )
      )
    )

    val response = authenticatedPostRequest("/generate-revocation-order", RevocationOrderRequest(nomsNumber))

    assertThat(response.content, equalTo(expectedBase64Pdf))
  }

  private fun authenticatedPostRequest(path: String, request: RevocationOrderRequest): Pdf =
    sendAuthenticatedPostRequestWithBody(path, request)
      .expectStatus().isOk
      .expectBody(Pdf::class.java)
      .returnResult()
      .responseBody!!
}
