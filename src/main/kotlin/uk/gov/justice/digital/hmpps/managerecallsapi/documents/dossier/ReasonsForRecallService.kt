package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REASONS_FOR_RECALL
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class ReasonsForRecallService(
  @Autowired private val dossierContextFactory: DossierContextFactory,
  @Autowired private val reasonsForRecallGenerator: ReasonsForRecallGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val pdfDecorator: PdfDecorator,
  @Autowired private val documentService: DocumentService,
) {

  fun getOrGeneratePdf(dossierContext: DossierContext, currentUserId: UserId): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(dossierContext.recall.recallId(), REASONS_FOR_RECALL)
      ?.let { Mono.just(it) }
      ?: dossierContext.getReasonsForRecallContext().let { ctx ->
        generateAndStorePdf(dossierContext.recall.recallId(), ctx, currentUserId, ctx.fileName()).map { it.second }
      }

  fun generateAndStorePdf(recallId: RecallId, currentUserId: UserId, fileName: FileName, details: String?): Mono<DocumentId> =
    dossierContextFactory.createContext(recallId).getReasonsForRecallContext().let { ctx ->
      generateAndStorePdf(recallId, ctx, currentUserId, fileName, details)
    }.map { it.first }

  private fun generateAndStorePdf(recallId: RecallId, reasonsForRecallContext: ReasonsForRecallContext, currentUserId: UserId, fileName: FileName, documentDetails: String? = null): Mono<Pair<DocumentId, ByteArray>> =
    pdfDocumentGenerationService.generatePdf(
      reasonsForRecallGenerator.generateHtml(reasonsForRecallContext),
      1.0, 0.8
    ).map { reasonsForRecallBytes ->
      pdfDecorator.centralHeader(reasonsForRecallBytes, "OFFICIAL")
    }.map { documentBytes ->
      val documentId = documentService.storeDocument(
        recallId,
        currentUserId,
        documentBytes,
        REASONS_FOR_RECALL,
        fileName,
        documentDetails
      )
      Pair(documentId, documentBytes)
    }
}
