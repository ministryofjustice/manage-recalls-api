package uk.gov.justice.digital.hmpps.managerecallsapi.integration.documents

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.Pdf
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.managerecallsapi.integration.mockservers.GotenbergMockServer
import java.util.Base64

class GenerateRevocationOrderTest : IntegrationTestBase() {

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
    sendAuthenticatedPostRequest("/generate-revocation-order", invalidUserJwt)
      .expectStatus().isUnauthorized
  }

  @Test
  fun `generate revocation order endpoint should return generated pdf`() {
    val expectedPdf = "Expected Generated PDF".toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)
    gotenbergMockServer.stubPdfGeneration(expectedPdf)

    val response = authenticatedPostRequest("/generate-revocation-order")

    assertThat("Generated PDF does not match", response.content, equalTo(expectedBase64Pdf))
  }

  private fun authenticatedPostRequest(path: String): Pdf =
    sendAuthenticatedPostRequest(path)
      .expectStatus().isOk
      .expectBody(Pdf::class.java)
      .returnResult()
      .responseBody!!
}
