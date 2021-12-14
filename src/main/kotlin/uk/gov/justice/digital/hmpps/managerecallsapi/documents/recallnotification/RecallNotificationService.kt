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

  fun getOrGeneratePdf(recallId: RecallId, currentUserId: UserId): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION)
      ?.let { Mono.just(it) }
      ?: generateAndStorePdf(recallId, currentUserId).map { it.second }

  fun generateAndStorePdf(recallId: RecallId, currentUserId: UserId, details: String? = null): Mono<Pair<DocumentId, ByteArray>> {
    val recallNotificationContext = recallNotificationContextFactory.createContext(recallId, currentUserId)

    val documentGenerators = Flux.just(
      { recallSummaryService.generatePdf(recallNotificationContext) },
      { revocationOrderService.getOrGeneratePdf(recallNotificationContext.getRevocationOrderContext()) },
      { letterToProbationService.generatePdf(recallNotificationContext) }
    )
    return documentGenerators
      .flatMapSequential { it() }
      .map { documentData(it) }
      .collectList()
      .flatMap { pdfDocumentGenerationService.mergePdfs(it) }
      .map { mergedBytes ->
        val documentId = documentService.storeDocument(recallId, currentUserId, mergedBytes, RECALL_NOTIFICATION, "$RECALL_NOTIFICATION.pdf")
        Pair(documentId, mergedBytes)
      }
  }
}
