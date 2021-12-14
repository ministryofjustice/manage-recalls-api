package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo

@Service
class TableOfContentsService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val tableOfContentsGenerator: TableOfContentsGenerator,
) {

  fun generatePdf(dossierContext: DossierContext, contentDocs: Map<String, ByteArrayDocumentData>): Mono<ByteArray> =
    pdfDocumentGenerationService.generatePdf(
      tableOfContentsGenerator.generateHtml(
        dossierContext.getTableOfContentsContext(),
        getTableOfContentsItems(contentDocs)
      ),
      recallImage(HmppsLogo)
    )

  private fun getTableOfContentsItems(contentDocs: Map<String, ByteArrayDocumentData>): List<TableOfContentsItem> {
    var currentPage = 1
    return contentDocs.map { (name, content) ->
      val doc = TableOfContentsItem(name, currentPage)
      currentPage += PdfReader(content.byteArray).numberOfPages
      doc
    }
  }
}
