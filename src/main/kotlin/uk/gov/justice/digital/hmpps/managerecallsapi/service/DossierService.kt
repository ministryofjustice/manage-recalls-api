package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId

@Service
class DossierService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Autowired private val pdfDecorator: PdfDecorator,
) {

  fun getDossier(recallId: RecallId): Mono<ByteArray> {
    val license = recallDocumentService.getDocumentContentWithCategory(recallId, LICENCE)
    val partARecallReport = recallDocumentService.getDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT)
    val revocationOrder = recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER)

    val docs = mutableListOf(
      InputStreamDocumentData(license.inputStream()),
      InputStreamDocumentData(partARecallReport.inputStream()),
      InputStreamDocumentData(revocationOrder.inputStream())
    )

    return pdfDocumentGenerationService.mergePdfs(docs)
      .map { mergedPdfBytes ->
        pdfDecorator.numberPages(mergedPdfBytes)
      }
  }
}
