package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

// This is the number of pages of the Revocation Order and Letter to Probation
const val OTHER_PAGES_IN_RECALL_NOTIFICATION = 2

@Service
class RecallSummaryService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallSummaryGenerator: RecallSummaryGenerator
) {

  fun createPdf(recallNotificationContext: RecallNotificationContext): Mono<ByteArray> =
    recallNotificationContext.getRecallSummaryContext().let { recallSummaryContext ->
      getRecallSummaryNumberOfPages(recallSummaryContext).map { numberOfPages ->
        recallSummaryContext to OTHER_PAGES_IN_RECALL_NOTIFICATION + numberOfPages
      }.flatMap { contextWithActualNumberOfPages ->
        generatePdf(contextWithActualNumberOfPages.first, contextWithActualNumberOfPages.second)
      }
    }

  private fun getRecallSummaryNumberOfPages(recallSummaryContext: RecallSummaryContext) =
    generatePdf(recallSummaryContext).map { pdfBytes ->
      PdfReader(pdfBytes).use { it.numberOfPages }
    }

  private fun generatePdf(recallSummaryContext: RecallSummaryContext, recallNotificationTotalNumberOfPages: Int? = null) =
    pdfDocumentGenerationService.generatePdf(
      recallSummaryGenerator.generateHtml(recallSummaryContext, recallNotificationTotalNumberOfPages),
      1.0,
      1.0,
      recallImage(HmppsLogo)
    )
}
