package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.DocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId

@Service
class RecallNotificationService(
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val recallSummaryService: RecallSummaryService,
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator
) {

  fun getDocument(recallId: RecallId): Mono<ByteArray> {
    val docs = mutableListOf<DocumentDetail<out Any>>()
    return recallSummaryService.getPdf(recallId).map { recallSummaryBytes ->
      docs.add(InputStreamDocumentDetail("1-recallSummary.pdf", recallSummaryBytes.inputStream()))
    }.flatMap {
      revocationOrderService.getPdf(recallId)
    }.flatMap { revocationOrderBytes ->
      docs.add(InputStreamDocumentDetail("2-revocationOrder.pdf", revocationOrderBytes.inputStream()))
      pdfDocumentGenerator.mergePdfs(docs)
    }
  }
}
