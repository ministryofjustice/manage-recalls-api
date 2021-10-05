package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo

@Suppress("ReactiveStreamsUnusedPublisher")
internal class TableOfContentsServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val tableOfContentsGenerator = mockk<TableOfContentsGenerator>()

  private val underTest = TableOfContentsService(pdfDocumentGenerationService, tableOfContentsGenerator)

  @Test
  fun `get table of contents for supplied documents`() {
    val documentStream1 = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val documentStream2 = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()

    val dossierContext = mockk<DossierContext>()
    val tableOfContentsContext = mockk<TableOfContentsContext>()
    val someHtml = "Some content"

    val tableOfContentsItems = listOf(
      TableOfContentsItem("Document 1", 1),
      TableOfContentsItem("Document 2", 4)
    )
    every { dossierContext.getTableOfContentsContext() } returns tableOfContentsContext
    every { tableOfContentsGenerator.generateHtml(tableOfContentsContext, tableOfContentsItems) } returns someHtml

    val tocBytes = "Some bytes".toByteArray()
    every { pdfDocumentGenerationService.generatePdf(someHtml, recallImage(HmppsLogo)) } returns Mono.just(tocBytes)

    val tableOfContents = underTest.createPdf(
      dossierContext,
      mapOf(
        "Document 1" to ByteArrayDocumentData(documentStream1),
        "Document 2" to ByteArrayDocumentData(documentStream2)
      )
    ).block()!!

    assertThat(tableOfContents, equalTo(tocBytes))
  }
}
