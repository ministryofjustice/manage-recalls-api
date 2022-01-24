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
      context to otherPageInRecallNotification(context.inCustody) + numberOfPages
    }.flatMap { contextWithActualNumberOfPages ->
      generatePdfContent(contextWithActualNumberOfPages.first, contextWithActualNumberOfPages.second)
    }

  private fun otherPageInRecallNotification(inCustody: Boolean): Int {
    // This is the number of pages of the Revocation Order and Letter to Probation. but not in Custody has 2 other documents
    return if (inCustody) 2 else 4
  }

  private fun getRecallSummaryNumberOfPages(recallSummaryContext: RecallSummaryContext) =
    generatePdfContent(recallSummaryContext).map { pdfBytes ->
      PdfReader(pdfBytes).use { it.numberOfPages }
    }

  private fun generatePdfContent(recallSummaryContext: RecallSummaryContext, recallNotificationTotalNumberOfPages: Int? = null) =
    pdfDocumentGenerationService.generatePdf(
      recallSummaryGenerator.generateHtml(recallSummaryContext, recallNotificationTotalNumberOfPages),
      1.0,
      1.0,
      recallImage(HmppsLogo)
    )
}
