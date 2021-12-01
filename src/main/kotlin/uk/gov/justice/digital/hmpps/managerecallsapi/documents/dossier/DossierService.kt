package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Data.Companion.documentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.RecallInformationLeaflet
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.io.InputStream

@Service
class DossierService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val documentService: DocumentService,
  @Autowired private val reasonsForRecallService: ReasonsForRecallService,
  @Autowired private val pdfDecorator: PdfDecorator,
  @Autowired private val tableOfContentsService: TableOfContentsService,
  @Autowired private val dossierContextFactory: DossierContextFactory
) {

  fun getPdf(recallId: RecallId, createdByUserId: UserId): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, DOSSIER)
      ?.let { Mono.just(it) }
      ?: createDossier(recallId, createdByUserId)

  private fun createDossier(recallId: RecallId, createdByUserId: UserId): Mono<ByteArray> {
    val dossierContext = dossierContextFactory.createContext(recallId)

    return reasonsForRecallService.getPdf(dossierContext, createdByUserId).map { reasonsForRecallPdfBytes ->
      createTableOfContentsDocumentMap(recallId, reasonsForRecallPdfBytes)
    }.flatMap { tableOfContentsDocumentMap ->
      tableOfContentsService.createPdf(dossierContext, tableOfContentsDocumentMap).map { tableOfContentsBytes ->
        mutableListOf(documentData(tableOfContentsBytes)).apply {
          this += tableOfContentsDocumentMap.values
        }
      }
    }.flatMap { dossierDocuments ->
      pdfDocumentGenerationService.mergePdfs(dossierDocuments)
    }.map { mergedPdfContentBytes ->
      pdfDecorator.numberPages(mergedPdfContentBytes, numberOfPagesToSkip = 1)
    }.map { mergedBytes ->
      documentService.storeDocument(recallId, createdByUserId, mergedBytes, DOSSIER, "$DOSSIER.pdf")
      mergedBytes
    }
  }

  private fun createTableOfContentsDocumentMap(
    recallId: RecallId,
    reasonsForRecall: ByteArray
  ): MutableMap<String, ByteArrayDocumentData> {
    val license = documentService.getLatestVersionedDocumentContentWithCategory(recallId, LICENCE)
    val partARecallReport = documentService.getLatestVersionedDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT)
    val revocationOrder = documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER)
    return mutableMapOf(
      "Recall Information Leaflet [Core Dossier]" to documentData(RecallInformationLeaflet),
      "Licence [Core Dossier]" to documentData(license),
      "Request for Recall Report [Core Dossier]" to documentData(partARecallReport),
      "Revocation Order [Core Dossier]" to documentData(revocationOrder),
      "Reasons for Recall [Core Dossier]" to documentData(reasonsForRecall)
    )
  }
}

enum class RecallClassPathResource(private val path: String) {
  RecallInformationLeaflet("/pdfs/recall-information-leaflet.pdf");

  fun inputStream(): InputStream = ClassPathResource(path).inputStream
  fun byteArray(): ByteArray = ClassPathResource(path).inputStream.readAllBytes()
}
