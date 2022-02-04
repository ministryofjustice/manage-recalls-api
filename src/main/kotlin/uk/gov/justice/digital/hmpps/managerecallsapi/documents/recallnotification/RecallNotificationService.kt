package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
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
  @Autowired private val offenderNotificationService: OffenderNotificationService,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val documentService: DocumentService,
) {

  fun generateAndStorePdf(recallId: RecallId, currentUserId: UserId, details: String?): Mono<DocumentId> {
    val recallNotificationContext = recallNotificationContextFactory.createContext(recallId, currentUserId)

    val documentGeneratorList: Array<() -> Mono<ByteArray>> = listOf(
      { recallSummaryService.generatePdf(recallNotificationContext.getRecallSummaryContext()) },
      { revocationOrderService.getOrGeneratePdf(recallNotificationContext.getRevocationOrderContext()) }
    ).plus(getNotInCustodyDocumentGenerators(recallNotificationContext))
      .plus { letterToProbationService.generatePdf(recallNotificationContext.getLetterToProbationContext()) }
      .toTypedArray()

    val documentGenerators: Flux<() -> Mono<ByteArray>> = Flux.just(*documentGeneratorList)
    return documentGenerators
      .flatMapSequential { it() }
      .map { documentData(it) }
      .collectList()
      .flatMap { pdfDocumentGenerationService.mergePdfs(it) }
      .map { mergedBytes ->
        documentService.storeDocument(recallId, currentUserId, mergedBytes, RECALL_NOTIFICATION, "$RECALL_NOTIFICATION.pdf", details)
      }
  }

  private fun getNotInCustodyDocumentGenerators(context: RecallNotificationContext): List<() -> Mono<ByteArray>> {
    return if (!context.recall.inCustodyRecall()) {
      listOf(
        { offenderNotificationService.generatePdf(context.getOffenderNotificationContext()) },
        { Mono.just(ClassPathResource("/pdfs/Police-Notification.pdf").inputStream.readAllBytes()) }
      )
    } else {
      emptyList()
    }
  }
}
