package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Service
class LetterToProbationService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val letterToProbationGenerator: LetterToProbationGenerator,
  @Autowired private val letterToProbationContextFactory: LetterToProbationContextFactory,
) {
  fun getPdf(recallId: RecallId, userId: UserId): Mono<ByteArray> =
    letterToProbationContextFactory.createContext(recallId, userId)
      .map { context ->
        letterToProbationGenerator.generateHtml(context)
      }.flatMap { letterToProbationHtml ->
        pdfDocumentGenerationService.generatePdf(
          letterToProbationHtml,
          recallImage(HmppsLogo)
        )
      }
}
