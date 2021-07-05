package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.thymeleaf.spring5.SpringTemplateEngine
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import java.util.Base64

internal class GenerateRevocationOrderControllerTest {

  private val pdfDocumentGenerator = mockk<PdfDocumentGenerator>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()
  private val thymeleafConfig = mockk<SpringTemplateEngine>()

  private val underTest = GenerateRevocationOrderController(pdfDocumentGenerator, prisonerOffenderSearchClient, thymeleafConfig)

  @Test
  fun `generates a revocation order using the pdf generator`() {
    val expectedPdf = "Some pdf".toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

    every { prisonerOffenderSearchClient.prisonerSearch(any()) } returns Mono.just(listOf(Prisoner()))
    every { pdfDocumentGenerator.makePdf(any()) } returns Mono.just(expectedPdf)
    every { thymeleafConfig.process("revocation-order", any()) } returns "Some html, honest"

    val result = underTest.generateRevocationOrder(RevocationOrderRequest("My Noms Number"))

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body.content, equalTo(expectedBase64Pdf))
      }
      .verifyComplete()
  }
}
