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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.FixedTermRecallInformationLeafletEnglish
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.FixedTermRecallInformationLeafletWelsh
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
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

  fun getOrGeneratePdf(recallId: RecallId, currentUserId: UserId): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, DOSSIER)
      ?.let { Mono.just(it) }
      ?: generatePdf(recallId, currentUserId).map { it.second }

  fun generateAndStorePdf(recallId: RecallId, currentUserId: UserId, documentDetails: String): Mono<DocumentId> =
    generatePdf(recallId, currentUserId, documentDetails).map { it.first }

  private fun generatePdf(recallId: RecallId, currentUserId: UserId, documentDetails: String? = null): Mono<Pair<DocumentId, ByteArray>> {
    val dossierContext = dossierContextFactory.createContext(recallId)

    return reasonsForRecallService.getOrGeneratePdf(dossierContext, currentUserId).map { reasonsForRecallPdfBytes ->
      createTableOfContentsDocumentMap(reasonsForRecallPdfBytes, recallId, dossierContext.includeWelsh())
    }.flatMap { tableOfContentsDocumentMap ->
      tableOfContentsService.generatePdf(dossierContext, tableOfContentsDocumentMap).map { tableOfContentsBytes ->
        mutableListOf(documentData(tableOfContentsBytes)).apply {
          this += tableOfContentsDocumentMap.values
        }
      }
    }.flatMap { dossierDocuments ->
      pdfDocumentGenerationService.mergePdfs(dossierDocuments)
    }.map { mergedPdfContentBytes ->
      pdfDecorator.numberPages(mergedPdfContentBytes, numberOfPagesToSkip = 1)
    }.map { mergedBytes ->
      val documentId = documentService.storeDocument(recallId, currentUserId, mergedBytes, DOSSIER, "$DOSSIER.pdf", documentDetails)
      Pair(documentId, mergedBytes)
    }
  }

  private fun createTableOfContentsDocumentMap(
    reasonsForRecall: ByteArray,
    recallId: RecallId,
    includeWelsh: Boolean
  ): MutableMap<String, ByteArrayDocumentData> {
    val license = documentService.getLatestVersionedDocumentContentWithCategory(recallId, LICENCE)
    val partARecallReport = documentService.getLatestVersionedDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT)
    val revocationOrder = documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER)
    val documents = mutableMapOf(
      "Recall Information Leaflet [Core Dossier]" to documentData(FixedTermRecallInformationLeafletEnglish)
    )
    if (includeWelsh) {
      documents["Welsh Recall Information Leaflet"] = documentData(FixedTermRecallInformationLeafletWelsh)
    }
    documents.putAll(
      mutableMapOf(
        "Recall Information Leaflet [Core Dossier]" to documentData(FixedTermRecallInformationLeafletEnglish),
        "Licence [Core Dossier]" to documentData(license),
        "Request for Recall Report [Core Dossier]" to documentData(partARecallReport),
        "Revocation Order [Core Dossier]" to documentData(revocationOrder),
        "Reasons for Recall [Core Dossier]" to documentData(reasonsForRecall)
      )
    )
    return documents
  }
}

enum class RecallClassPathResource(private val path: String) {
  FixedTermRecallInformationLeafletEnglish("/pdfs/FTR-recall-information-leaflet-English.pdf"),
  FixedTermRecallInformationLeafletWelsh("/pdfs/FTR-recall-information-leaflet-Welsh.pdf");

  fun inputStream(): InputStream = ClassPathResource(path).inputStream
  fun byteArray(): ByteArray = ClassPathResource(path).inputStream.readAllBytes()
}
