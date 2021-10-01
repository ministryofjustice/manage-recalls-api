package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Service
class LetterToProbationService(
  @Autowired private val letterToProbationContextFactory: LetterToProbationContextFactory,
  @Autowired private val letterToProbationGenerator: LetterToProbationGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
) {
  fun createPdf(recallNotificationContext: RecallNotificationContext): Mono<ByteArray> {
    val letterToProbationContext = letterToProbationContextFactory.createContext(recallNotificationContext)
    val letterToProbationHtml = letterToProbationGenerator.generateHtml(letterToProbationContext)
    return pdfDocumentGenerationService.generatePdf(letterToProbationHtml, recallImage(HmppsLogo))
  }
}
