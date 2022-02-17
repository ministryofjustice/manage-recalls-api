package uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo

@Service
class RecallSummaryService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallSummaryGenerator: RecallSummaryGenerator
) {

  fun generatePdf(context: RecallSummaryContext): Mono<ByteArray> =
    getRecallSummaryNumberOfPages(context).map { numberOfPages ->
      context to ( // This is the number of pages of the Revocation Order and Letter to Probation. but not in Custody has 2 other documents
        if (context.inCustodyRecall) 2 else 4
        ) + numberOfPages
    }.flatMap { contextWithActualNumberOfPages ->
      generatePdfContent(contextWithActualNumberOfPages.first, contextWithActualNumberOfPages.second)
    }

  private fun getRecallSummaryNumberOfPages(recallSummaryContext: RecallSummaryContext) =
    generatePdfContent(recallSummaryContext).map { pdfBytes ->
      PdfReader(pdfBytes).use { it.numberOfPages }
    }

  private fun generatePdfContent(recallSummaryContext: RecallSummaryContext, recallNotificationTotalNumberOfPages: Int? = null) =
    pdfDocumentGenerationService.generatePdf(
      recallSummaryGenerator.generateHtml(recallSummaryContext, recallNotificationTotalNumberOfPages),
      1.0,
      0.8,
      recallImage(HmppsLogo)
    )
}
