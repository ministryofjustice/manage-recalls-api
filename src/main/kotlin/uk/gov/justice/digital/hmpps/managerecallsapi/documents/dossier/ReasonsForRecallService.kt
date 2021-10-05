package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DossierContext

@Service
class ReasonsForRecallService(
  @Autowired private val reasonsForRecallGenerator: ReasonsForRecallGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
) {
  fun createPdf(dossierContext: DossierContext): Mono<ByteArray> =
    pdfDocumentGenerationService.generatePdf(
      reasonsForRecallGenerator.generateHtml(dossierContext.getReasonsForRecallContext())
    )
}
