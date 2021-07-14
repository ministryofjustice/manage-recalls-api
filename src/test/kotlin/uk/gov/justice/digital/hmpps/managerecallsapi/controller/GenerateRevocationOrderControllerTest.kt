package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.util.Base64

internal class GenerateRevocationOrderControllerTest {

  private val revocationOrderService = mockk<RevocationOrderService>()

  private val underTest = GenerateRevocationOrderController(revocationOrderService)

  @Test
  fun `generates a revocation order using the pdf generator`() {
    val expectedPdf = "Some pdf".toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

    every { revocationOrderService.generateRevocationOrder(any()) } returns Mono.just(expectedPdf)

    val result = underTest.generateRevocationOrder(RevocationOrderRequest("My Noms Number"))

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body?.content, equalTo(expectedBase64Pdf))
      }
      .verifyComplete()
  }
}
