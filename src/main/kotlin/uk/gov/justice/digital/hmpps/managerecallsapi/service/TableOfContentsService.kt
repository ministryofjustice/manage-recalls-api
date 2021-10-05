package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Service
class TableOfContentsService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val tableOfContentsGenerator: TableOfContentsGenerator,
) {

  fun createPdf(dossierContext: DossierContext, contentDocs: Map<String, ByteArrayDocumentData>): Mono<ByteArray> {
    val docList = generateDocsList(contentDocs)
    val recall = dossierContext.recall
    val currentPrisonName = dossierContext.currentPrisonName
    val prisoner = dossierContext.prisoner

    return pdfDocumentGenerationService.generatePdf(
      tableOfContentsGenerator.generateHtml(
        TableOfContentsContext(
          recall,
          prisoner,
          currentPrisonName,
          docList
        )
      ),
      recallImage(HmppsLogo)
    )
  }

  private fun generateDocsList(contentDocs: Map<String, ByteArrayDocumentData>): List<Document> {
    var currentPage = 1
    return contentDocs.map { (name, content) ->
      val doc = Document(name, currentPage)
      currentPage += PdfReader(content.byteArray).numberOfPages
      doc
    }
  }
}

data class Document(val title: String, val pageNumber: Int)
data class TableOfContentsContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val currentPrisonName: PrisonName,
  val documents: List<Document>
)
