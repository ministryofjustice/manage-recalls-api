package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Suppress("ReactiveStreamsUnusedPublisher")
internal class TableOfContentsServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val tableOfContentsGenerator = mockk<TableOfContentsGenerator>()

  private val underTest = TableOfContentsService(pdfDocumentGenerationService, tableOfContentsGenerator)

  @Test
  fun `get table of contents for supplied documents`() {
    val documentStream1 = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val documentStream2 = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()

    val recallId = ::RecallId.random()
    val prisoner = Prisoner()
    val nomsNumber = NomsNumber("AB1234C")
    val recall = Recall(recallId, nomsNumber, currentPrison = PrisonId("ABC"))
    val currentPrisonName = PrisonName("Current Prison")
    val someHtml = "Some content"

    val dossierContext = DossierContext(recall, prisoner, currentPrisonName)

    val tableOfContentsDocuments = listOf(
      Document("Document 1", 1),
      Document("Document 2", 4)
    )
    val tableOfContentsContext = TableOfContentsContext(recall, prisoner, currentPrisonName, tableOfContentsDocuments)
    every { tableOfContentsGenerator.generateHtml(tableOfContentsContext) } returns someHtml

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
