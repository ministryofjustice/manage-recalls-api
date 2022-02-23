package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.signature
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.RevocationOrderLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class RevocationOrderService(
  @Autowired private val recallNotificationContextFactory: RecallNotificationContextFactory,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val documentService: DocumentService,
  @Autowired private val revocationOrderGenerator: RevocationOrderGenerator,
) {

  fun generateAndStorePdf(recallId: RecallId, currentUserId: UserId, fileName: FileName, details: String?): Mono<DocumentId> =
    recallNotificationContextFactory.createContext(recallId, currentUserId).getRevocationOrderContext().let { ctx ->
      generateAndStorePdf(ctx, fileName, details).map { it.first }
    }

  private fun generateAndStorePdf(revocationOrderContext: RevocationOrderContext, fileName: FileName = revocationOrderContext.fileName(), documentDetails: String? = null): Mono<Pair<DocumentId, ByteArray>> =
    revocationOrderContext.let { context ->
      pdfDocumentGenerationService.generatePdf(
        revocationOrderGenerator.generateHtml(context),
        recallImage(RevocationOrderLogo),
        signature(context.currentUserSignature)
      ).map { bytes ->
        val documentId = documentService.storeDocument(context.recallId, context.currentUserId, bytes, REVOCATION_ORDER, fileName, documentDetails)
        Pair(documentId, bytes)
      }
    }

  fun getOrGeneratePdf(context: RevocationOrderContext): Mono<ByteArray> =
    documentService.getLatestVersionedDocumentContentWithCategoryIfExists(context.recallId, REVOCATION_ORDER)
      ?.let { Mono.just(it) }
      ?: generateAndStorePdf(context).map { it.second }
}
