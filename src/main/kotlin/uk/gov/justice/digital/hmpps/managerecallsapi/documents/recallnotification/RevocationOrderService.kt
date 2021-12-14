package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.signature
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.RevocationOrderLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class RevocationOrderService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val documentService: DocumentService,
  @Autowired private val revocationOrderGenerator: RevocationOrderGenerator,
) {

  private fun generateAndStorePdf(revocationOrderContext: RevocationOrderContext, createdByUserId: UserId): Mono<ByteArray> =
    revocationOrderContext.let { context ->
      pdfDocumentGenerationService.generatePdf(
        revocationOrderGenerator.generateHtml(context),
        recallImage(RevocationOrderLogo),
        signature(context.assessedByUserSignature)
      ).map { bytes ->
        documentService.storeDocument(context.recallId, createdByUserId, bytes, REVOCATION_ORDER, "$REVOCATION_ORDER.pdf")
        bytes
      }
    }

  fun getOrGeneratePdf(context: RevocationOrderContext, createdByUserId: UserId): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(context.recallId, REVOCATION_ORDER)
      ?.let { Mono.just(it) }
      ?: generateAndStorePdf(context, createdByUserId)
}
