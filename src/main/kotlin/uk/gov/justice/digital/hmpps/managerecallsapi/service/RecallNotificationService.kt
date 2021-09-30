package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Data.Companion.documentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId

@Service
class RecallNotificationService(
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val recallSummaryService: RecallSummaryService,
  @Autowired private val letterToProbationService: LetterToProbationService,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallDocumentService: RecallDocumentService,
) {

  fun getDocument(recallId: RecallId, userId: UserId): Mono<ByteArray> =
    recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, RECALL_NOTIFICATION)
      ?.let { Mono.just(it) }
      ?: createRecallNotification(recallId, userId)

  private fun createRecallNotification(recallId: RecallId, userId: UserId): Mono<ByteArray> {
    val docs = mutableListOf<ByteArrayDocumentData>()
    // TODO:  Remove this block() (same problem as comment in the DossierService)
    val letterToProbationBytes = letterToProbationService.getPdf(recallId, userId).block()!!

    return recallSummaryService.getPdf(recallId, userId).map { recallSummaryBytes ->
      docs += documentData(recallSummaryBytes)
    }.flatMap {
      revocationOrderService.createPdf(recallId, userId)
    }.map { revocationOrderBytes ->
      docs += documentData(revocationOrderBytes)
    }.map {
      docs += documentData(letterToProbationBytes)
    }.flatMap {
      pdfDocumentGenerationService.mergePdfs(docs)
    }.map { mergedBytes ->
      recallDocumentService.uploadAndAddDocumentForRecall(recallId, mergedBytes, RECALL_NOTIFICATION)
      mergedBytes
    }
  }
}
