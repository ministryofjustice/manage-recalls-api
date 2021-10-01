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
    getRecallSummaryNumberOfPages(recallNotificationContext).map { numberOfPages ->
      recallNotificationContext to OTHER_PAGES_IN_RECALL_NOTIFICATION + numberOfPages
    }.flatMap { contextWithActualNumberOfPages ->
      generatePdf(contextWithActualNumberOfPages.first, contextWithActualNumberOfPages.second)
    }

  private fun getRecallSummaryNumberOfPages(context: RecallNotificationContext) =
    generatePdf(context).map { pdfBytes ->
      PdfReader(pdfBytes).use { it.numberOfPages }
    }

  private fun generatePdf(context: RecallNotificationContext, recallNotificationTotalNumberOfPages: Int? = null) =
    pdfDocumentGenerationService.generatePdf(
      recallSummaryGenerator.generateHtml(context, recallNotificationTotalNumberOfPages),
      recallImage(HmppsLogo)
    )
}
