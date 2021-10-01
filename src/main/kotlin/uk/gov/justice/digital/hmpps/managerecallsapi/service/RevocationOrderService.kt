package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.signature
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.RevocationOrderLogo

@Service
class RevocationOrderService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Autowired private val revocationOrderGenerator: RevocationOrderGenerator,
) {

  fun createPdf(recallNotificationContext: RecallNotificationContext): Mono<ByteArray> {
    val revocationOrderHtml = revocationOrderGenerator.generateHtml(recallNotificationContext.prisoner, recallNotificationContext.recall)

    return pdfDocumentGenerationService.generatePdf(
      revocationOrderHtml,
      recallImage(RevocationOrderLogo),
      signature(recallNotificationContext.assessedByUserDetails.signature)
    ).map { bytes ->
      recallDocumentService.uploadAndAddDocumentForRecall(recallNotificationContext.recall.recallId(), bytes, REVOCATION_ORDER)
      bytes
    }
  }

  fun getPdf(recallId: RecallId): Mono<ByteArray> =
    Mono.just(recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER))
}
