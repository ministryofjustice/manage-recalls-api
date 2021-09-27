package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.TableOfContentsLogo

@Service
class TableOfContentsService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val tableOfContentsGenerator: TableOfContentsGenerator,
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val prisonLookupService: PrisonLookupService,
  @Autowired private val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {

  fun getPdf(recallId: RecallId, contentDocs: Map<String, ByteArrayDocumentData>): Mono<ByteArray> {
    val recall = recallRepository.getByRecallId(recallId)
    val currentPrisonName = prisonLookupService.getPrisonName(recall.currentPrison)!!

    val docList = generateDocsList(contentDocs)

    return prisonerOffenderSearchClient.prisonerSearch(SearchRequest(recall.nomsNumber))
      .flatMap { prisoners ->
        val tocHtml = tableOfContentsGenerator.generateHtml(TableOfContentsContext(recall, prisoners.first(), currentPrisonName, docList))

        pdfDocumentGenerationService.generatePdf(
          tocHtml,
          ClassPathImageData(TableOfContentsLogo)
        )
      }
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
data class TableOfContentsContext(val recall: Recall, val prisoner: Prisoner, val currentPrisonName: String, val documents: List<Document>)
