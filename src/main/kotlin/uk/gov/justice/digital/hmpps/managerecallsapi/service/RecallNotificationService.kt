package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentData
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

  fun getDocument(recallId: RecallId, userId: UserId): Mono<ByteArray> {
    val recallNotification = recallDocumentService.getDocumentContentWithCategoryIfExists(
      recallId,
      RECALL_NOTIFICATION
    )

    if (recallNotification == null) {
      val docs = mutableListOf<InputStreamDocumentData>()
      return recallSummaryService.getPdf(recallId).map { recallSummaryBytes ->
        docs.add(InputStreamDocumentData(recallSummaryBytes.inputStream()))
      }.flatMap {
        revocationOrderService.createPdf(recallId, userId)
      }.map { revocationOrderBytes ->
        docs.add(InputStreamDocumentData(revocationOrderBytes.inputStream()))
      }.flatMap {
        letterToProbationService.getPdf(recallId)
      }.flatMap { letterToProbationBytes ->
        docs.add(InputStreamDocumentData(letterToProbationBytes.inputStream()))
        pdfDocumentGenerationService.mergePdfs(docs)
      }.map { mergedBytes ->
        recallDocumentService.uploadAndAddDocumentForRecall(recallId, mergedBytes, RECALL_NOTIFICATION)
        mergedBytes
      }
    } else {
      return Mono.just(recallNotification)
    }
  }
}
