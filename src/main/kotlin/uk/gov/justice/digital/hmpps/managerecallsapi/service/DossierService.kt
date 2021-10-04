package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Data.Companion.documentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallClassPathResource.RecallInformationLeaflet
import java.io.InputStream

@Service
class DossierService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Autowired private val reasonsForRecallService: ReasonsForRecallService,
  @Autowired private val pdfDecorator: PdfDecorator,
  @Autowired private val tableOfContentsService: TableOfContentsService,
  @Autowired private val dossierContextFactory: DossierContextFactory
) {

  fun getDossier(recallId: RecallId): Mono<ByteArray> {
    val license = recallDocumentService.getDocumentContentWithCategory(recallId, LICENCE)
    val partARecallReport = recallDocumentService.getDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT)
    val revocationOrder = recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER)
    val dossierContext = dossierContextFactory.createContext(recallId)

    // TODO: attempts to add this without block() fell over on runtime errors relating to e.g. other use of block in getPrisonName
    val reasonsForRecall = reasonsForRecallService.getPdf(dossierContext).block()!!

    val dossierDocuments = mutableMapOf(
      "Recall Information Leaflet [Core Dossier]" to documentData(RecallInformationLeaflet),
      "Licence [Core Dossier]" to documentData(license),
      "Request for Recall Report [Core Dossier]" to documentData(partARecallReport),
      "Revocation Order [Core Dossier]" to documentData(revocationOrder),
      "Reasons for Recall [Core Dossier]" to documentData(reasonsForRecall)
    )

    return tableOfContentsService.getPdf(recallId, dossierDocuments).map { tocBytes ->
      val tocAndDossierDocuments = mutableListOf(documentData(tocBytes))
      tocAndDossierDocuments.addAll(dossierDocuments.values)
      tocAndDossierDocuments
    }.flatMap {
      pdfDocumentGenerationService.mergePdfs(it)
    }.map { mergedPdfContentBytes ->
      pdfDecorator.numberPages(mergedPdfContentBytes, numberOfPagesToSkip = 1)
    }
  }
}

enum class RecallClassPathResource(private val path: String) {
  RecallInformationLeaflet("/pdfs/recall-information-leaflet.pdf");

  fun inputStream(): InputStream = ClassPathResource(path).inputStream
  fun byteArray(): ByteArray = ClassPathResource(path).inputStream.readAllBytes()
}
