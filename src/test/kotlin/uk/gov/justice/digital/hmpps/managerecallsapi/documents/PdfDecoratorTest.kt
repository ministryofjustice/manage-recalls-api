package uk.gov.justice.digital.hmpps.managerecallsapi.documents

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

internal class PdfDecoratorTest {

  val underTest = PdfDecorator()

  @Test
  fun `numberPages appends numeric 1 based page numbering to each page of input`() {

    val inputBytes = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()

    val actualReader = PdfReader(underTest.numberPages(inputBytes))

    val inputReader = PdfReader(inputBytes)
    val inputExtractor = PdfTextExtractor(inputReader)
    val actualExtractor = PdfTextExtractor(actualReader)
    assertThat(inputReader.numberOfPages, equalTo(3))
    assertThat(actualReader.numberOfPages, equalTo(3))
    assertThat(inputExtractor.getTextFromPage(1), equalTo("Unnumbered page 1 of three."))
    assertThat(actualExtractor.getTextFromPage(1), equalTo("Unnumbered page 1 of three. 1"))

    (1..3).asSequence().forEach {
      val inputPageText = "Unnumbered page $it of three."
      assertThat(inputExtractor.getTextFromPage(it).endsWith("$it"), equalTo(false))
      assertThat(actualExtractor.getTextFromPage(it).endsWith("$it"), equalTo(true))
      assertThat(inputExtractor.getTextFromPage(it), equalTo(inputPageText))
      assertThat(actualExtractor.getTextFromPage(it), equalTo("$inputPageText $it"))
    }
  }
}
