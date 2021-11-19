package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.signature
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.RevocationOrderLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class RevocationOrderService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val documentService: DocumentService,
  @Autowired private val revocationOrderGenerator: RevocationOrderGenerator,
) {

  fun createPdf(recallNotificationContext: RecallNotificationContext): Mono<ByteArray> =
    recallNotificationContext.getRevocationOrderContext()
      .let { revocationOrderContext ->
        pdfDocumentGenerationService.generatePdf(
          revocationOrderGenerator.generateHtml(revocationOrderContext),
          recallImage(RevocationOrderLogo),
          signature(revocationOrderContext.assessedByUserSignature)
        ).map { bytes ->
          documentService.storeDocument(revocationOrderContext.recallId, bytes, REVOCATION_ORDER, "$REVOCATION_ORDER.pdf")
          bytes
        }
      }

  fun getPdf(recallId: RecallId): Mono<ByteArray> =
    Mono.just(documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER))
}
