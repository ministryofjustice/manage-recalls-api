package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.StandardTermRecallInformationLeafletEnglish
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.StandardTermRecallInformationLeafletWelsh
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
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

  fun generateAndStorePdf(
    recallId: RecallId,
    currentUserId: UserId,
    fileName: FileName,
    documentDetails: String?
  ): Mono<DocumentId> {
    val dossierContext = dossierContextFactory.createContext(recallId)

    return reasonsForRecallService.getOrGeneratePdf(dossierContext, currentUserId).map { reasonsForRecallPdfBytes ->
      createTableOfContentsDocumentMap(reasonsForRecallPdfBytes, recallId, dossierContext.includeWelsh(), dossierContext.recallType)
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
      documentService.storeDocument(recallId, currentUserId, mergedBytes, DOSSIER, fileName, documentDetails)
    }
  }

  private fun createTableOfContentsDocumentMap(
    reasonsForRecall: ByteArray,
    recallId: RecallId,
    includeWelsh: Boolean,
    recallType: RecallType
  ): MutableMap<String, ByteArrayDocumentData> {
    val license = documentService.getLatestVersionedDocumentContentWithCategory(recallId, LICENCE)
    val partARecallReport = documentService.getLatestVersionedDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT)
    val revocationOrder = documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER)
    val documents = mutableMapOf(
      "Recall Information Leaflet [Core Dossier]" to fixedTermRecallInformationLeafletEnglish(recallType)
    )
    if (includeWelsh) {
      documents["Welsh Recall Information Leaflet"] = fixedTermRecallInformationLeafletWelsh(recallType)
    }
    documents.putAll(
      mutableMapOf(
        "Licence [Core Dossier]" to documentData(license),
        "Request for Recall Report [Core Dossier]" to documentData(partARecallReport),
        "Revocation Order [Core Dossier]" to documentData(revocationOrder),
        "Reasons for Recall [Core Dossier]" to documentData(reasonsForRecall)
      )
    )
    return documents
  }

  private fun fixedTermRecallInformationLeafletWelsh(recallType: RecallType) =
    when (recallType) {
      FIXED -> documentData(FixedTermRecallInformationLeafletWelsh)
      STANDARD -> documentData(StandardTermRecallInformationLeafletWelsh)
    }

  private fun fixedTermRecallInformationLeafletEnglish(recallType: RecallType) =
    when (recallType) {
      FIXED -> documentData(FixedTermRecallInformationLeafletEnglish)
      STANDARD -> documentData(StandardTermRecallInformationLeafletEnglish)
    }
}

enum class RecallClassPathResource(private val path: String) {
  FixedTermRecallInformationLeafletEnglish("/pdfs/Fixed-recall-information-leaflet-English.pdf"),
  FixedTermRecallInformationLeafletWelsh("/pdfs/Fixed-recall-information-leaflet-Welsh.pdf"),
  StandardTermRecallInformationLeafletEnglish("/pdfs/Standard-recall-information-leaflet-English.pdf"),
  StandardTermRecallInformationLeafletWelsh("/pdfs/Standard-recall-information-leaflet-Welsh.pdf");

  fun inputStream(): InputStream = ClassPathResource(path).inputStream
  fun byteArray(): ByteArray = ClassPathResource(path).inputStream.readAllBytes()
}
