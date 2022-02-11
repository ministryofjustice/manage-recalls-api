package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecallSummaryServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val recallSummaryGenerator = mockk<RecallSummaryGenerator>()

  private val underTest = RecallSummaryService(pdfDocumentGenerationService, recallSummaryGenerator)

  @ParameterizedTest(name = "inCustody = {0}")
  @MethodSource("parameterArrays")
  fun `generates the recall summary PDF with required information`(inCustody: Boolean, pageCount: Int) {
    val recallSummaryHtmlWithoutPageCount = "generated Html without page count"
    val recallSummaryHtmlWithPageCount = "generated Html with page count"
    val pdfWith3Pages = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val recallSummaryContext = mockk<RecallSummaryContext>()

    val pageCountSlot = slot<Int>()

    every { recallSummaryContext.inCustodyRecall } returns inCustody
    every { recallSummaryGenerator.generateHtml(recallSummaryContext, null) } returns recallSummaryHtmlWithoutPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithoutPageCount, 1.0, 0.8, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)
    every { recallSummaryGenerator.generateHtml(recallSummaryContext, capture(pageCountSlot)) } returns recallSummaryHtmlWithPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithPageCount, 1.0, 0.8, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)

    val result = underTest.generatePdf(recallSummaryContext).block()!!

    assertThat(pageCountSlot.captured, equalTo(pageCount))
    assertArrayEquals(pdfWith3Pages, result)
  }

  private fun parameterArrays(): Stream<Arguments>? =
    Stream.of(
      Arguments.of(true, 5),
      Arguments.of(false, 7),
    )
}
