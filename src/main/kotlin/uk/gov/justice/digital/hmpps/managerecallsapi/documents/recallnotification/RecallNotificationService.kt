package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Data.Companion.documentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class RecallNotificationService(
  @Autowired private val recallNotificationContextFactory: RecallNotificationContextFactory,
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val recallSummaryService: RecallSummaryService,
  @Autowired private val letterToProbationService: LetterToProbationService,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val documentService: DocumentService,
) {

  fun getOrCreatePdf(recallId: RecallId, createdByUserId: UserId): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION)
      ?.let { Mono.just(it) }
      ?: createAndStorePdf(recallId, createdByUserId).map { it.second }

  fun createAndStorePdf(recallId: RecallId, createdByUserId: UserId, details: String? = null): Mono<Pair<DocumentId, ByteArray>> {
    val recallNotificationContext = recallNotificationContextFactory.createContext(recallId, createdByUserId)

    val documentGenerators = Flux.just(
      { recallSummaryService.createPdf(recallNotificationContext) },
      { revocationOrderService.getOrCreatePdf(recallNotificationContext.getRevocationOrderContext(), createdByUserId) },
      { letterToProbationService.createPdf(recallNotificationContext) }
    )
    return documentGenerators
      .flatMapSequential { it() }
      .map { documentData(it) }
      .collectList()
      .flatMap { pdfDocumentGenerationService.mergePdfs(it) }
      .map { mergedBytes ->
        val documentId = documentService.storeDocument(recallId, createdByUserId, mergedBytes, RECALL_NOTIFICATION, "$RECALL_NOTIFICATION.pdf")
        Pair(documentId, mergedBytes)
      }
  }
}
