package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.DocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.Duration

@Service
class DossierService(
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator,
  @Autowired private val recallDocumentService: RecallDocumentService,
) {

  fun getDossier(recallId: RecallId): Mono<ByteArray> {
    val docs = mutableListOf<DocumentDetail<out Any>>()

    // For PUD-163 handling license or partA_RecallReport not present is out of scope
    val license = recallDocumentService.getDocumentContentWithCategory(recallId, RecallDocumentCategory.LICENCE)
    val partARecallReport = recallDocumentService.getDocumentContentWithCategory(recallId, RecallDocumentCategory.PART_A_RECALL_REPORT)

    docs.add(InputStreamDocumentDetail("3-license.pdf", license.inputStream()))
    docs.add(InputStreamDocumentDetail("6-partA_RecallReport.pdf", partARecallReport.inputStream()))

    // Warning from following block() should be addressed:
    revocationOrderService.getPdf(recallId).block(Duration.ofSeconds(5))?.let {
      docs.add(InputStreamDocumentDetail("9-revocationOrder.pdf", it.inputStream()))
    }

    return pdfDocumentGenerator.mergePdfs(docs)
  }
}
