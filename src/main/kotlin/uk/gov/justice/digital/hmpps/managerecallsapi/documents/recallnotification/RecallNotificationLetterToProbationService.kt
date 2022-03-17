package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo

@Service
class RecallNotificationLetterToProbationService(
  @Autowired private val recallNotificationLetterToProbationGenerator: RecallNotificationLetterToProbationGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
) {
  fun generatePdf(context: LetterToProbationContext): Mono<ByteArray> =
    pdfDocumentGenerationService.generatePdf(
      recallNotificationLetterToProbationGenerator.generateHtml(context),
      recallImage(HmppsLogo)
    )
}
