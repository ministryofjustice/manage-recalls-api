package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo

class RecallSummaryServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val recallSummaryGenerator = mockk<RecallSummaryGenerator>()

  private val underTest = RecallSummaryService(pdfDocumentGenerationService, recallSummaryGenerator)

  @Test
  fun `generates the recall summary PDF with required information`() {
    val recallSummaryHtmlWithoutPageCount = "generated Html without page count"
    val recallSummaryHtmlWithPageCount = "generated Html with page count"
    val pdfWith3Pages = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val recallNotificationContext = mockk<RecallNotificationContext>()
    val recallSummaryContext = mockk<RecallSummaryContext>()

    val pageCountSlot = slot<Int>()

    every { recallNotificationContext.getRecallSummaryContext() } returns recallSummaryContext
    every { recallSummaryGenerator.generateHtml(recallSummaryContext, null) } returns recallSummaryHtmlWithoutPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithoutPageCount, 1.0, 1.0, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)
    every { recallSummaryGenerator.generateHtml(recallSummaryContext, capture(pageCountSlot)) } returns recallSummaryHtmlWithPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithPageCount, 1.0, 1.0, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)

    val result = underTest.createPdf(recallNotificationContext).block()!!

    assertThat(pageCountSlot.captured, equalTo(5))
    assertArrayEquals(pdfWith3Pages, result)
  }
}
