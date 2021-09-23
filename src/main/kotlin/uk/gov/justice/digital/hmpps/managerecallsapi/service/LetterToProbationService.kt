package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId

@Service
class LetterToProbationService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val letterToProbationGenerator: LetterToProbationGenerator,
) {

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val letterToProbationHtml = letterToProbationGenerator.generateHtml()

    return pdfDocumentGenerationService.generatePdf(
      letterToProbationHtml,
      ClassPathImageData("letter-to-probation-logo.png")
    )
  }
}
