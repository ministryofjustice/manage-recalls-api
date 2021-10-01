package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId

@Service
class LetterToPrisonService(
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Autowired private val letterToPrisonGenerator: LetterToPrisonGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
) {

  fun getDocument(recallId: RecallId): Mono<ByteArray> {
    val letterToPrison = recallDocumentService.getDocumentContentWithCategoryIfExists(
      recallId,
      RecallDocumentCategory.LETTER_TO_PRISON
    )

    return if (letterToPrison == null) {
      getPdf(recallId).map { letterToPrisonBytes ->
        recallDocumentService.uploadAndAddDocumentForRecall(
          recallId, letterToPrisonBytes,
          RecallDocumentCategory.LETTER_TO_PRISON
        )
        letterToPrisonBytes
      }
    } else {
      Mono.just(letterToPrison)
    }
  }

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val letterToPrisonHtml = letterToPrisonGenerator.generateHtml()

    return pdfDocumentGenerationService.generatePdf(
      letterToPrisonHtml,
      ImageData.recallImage(RecallImage.RevocationOrderLogo)
    )
  }
}
