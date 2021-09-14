package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.DocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.Duration

@Service
class RecallNotificationService(
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val recallSummaryService: RecallSummaryService,
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator
) {

  fun getDocument(recallId: RecallId): Mono<ByteArray> {
    val docs = mutableListOf<DocumentDetail<out Any>>()

    recallSummaryService.getPdf(recallId).block(Duration.ofSeconds(5))?.let {
      docs.add(InputStreamDocumentDetail("1-recallSummary.pdf", it.inputStream()))
    }
    revocationOrderService.getPdf(recallId).block(Duration.ofSeconds(5))?.let {
      docs.add(InputStreamDocumentDetail("2-revocationOrder.pdf", it.inputStream()))
    }

    return pdfDocumentGenerator.mergePdfs(docs)
  }
}
