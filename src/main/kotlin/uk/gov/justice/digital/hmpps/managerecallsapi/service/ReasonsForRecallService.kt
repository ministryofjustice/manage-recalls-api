package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner

@Service
class ReasonsForRecallService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val reasonsForRecallGenerator: ReasonsForRecallGenerator,
) {

  fun getPdf(dossierContext: DossierContext): Mono<ByteArray> =
    pdfDocumentGenerationService.generatePdf(
      reasonsForRecallGenerator.generateHtml(
        ReasonsForRecallContext(
          dossierContext.recall, dossierContext.prisoner
        )
      )
    )
}

data class ReasonsForRecallContext(val recall: Recall, val prisoner: Prisoner)
