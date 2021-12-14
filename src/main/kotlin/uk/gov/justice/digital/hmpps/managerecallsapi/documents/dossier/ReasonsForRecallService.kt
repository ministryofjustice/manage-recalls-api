package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REASONS_FOR_RECALL
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class ReasonsForRecallService(
  @Autowired private val reasonsForRecallGenerator: ReasonsForRecallGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val pdfDecorator: PdfDecorator,
  @Autowired private val documentService: DocumentService,
) {

  fun getOrGeneratePdf(dossierContext: DossierContext, currentUserId: UserId): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(dossierContext.recall.recallId(), DocumentCategory.DOSSIER)
      ?.let { Mono.just(it) }
      ?: generatePdf(dossierContext, currentUserId)

  private fun generatePdf(dossierContext: DossierContext, currentUserId: UserId): Mono<ByteArray> =
    pdfDocumentGenerationService.generatePdf(
      reasonsForRecallGenerator.generateHtml(dossierContext.getReasonsForRecallContext()),
      1.0, 1.0
    ).map { reasonsForRecallBytes ->
      pdfDecorator.centralHeader(reasonsForRecallBytes, "OFFICIAL")
    }.map { documentBytes ->
      documentService.storeDocument(
        dossierContext.recall.recallId(),
        currentUserId,
        documentBytes,
        REASONS_FOR_RECALL,
        "$REASONS_FOR_RECALL.pdf"
      )
      documentBytes
    }
}
