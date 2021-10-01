package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

// This is the number of pages of the Revocation Order and Letter to Probation
const val OTHER_PAGES_IN_RECALL_NOTIFICATION = 2

@Service
class RecallSummaryService(
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val recallSummaryGenerator: RecallSummaryGenerator,
  @Autowired private val recallSummaryContextFactory: RecallSummaryContextFactory
) {

  fun getPdf(recallNotificationContext: RecallNotificationContext): Mono<ByteArray> =
    recallSummaryContextFactory.createRecallSummaryContext(recallNotificationContext).flatMap { context ->
      getRecallSummaryNumberOfPages(context).map { numberOfPages ->
        context.copy(recallNotificationTotalNumberOfPages = OTHER_PAGES_IN_RECALL_NOTIFICATION + numberOfPages)
      }.flatMap { contextWithActualNumberOfPages ->
        generatePdf(contextWithActualNumberOfPages)
      }
    }

  private fun getRecallSummaryNumberOfPages(context: RecallSummaryContext) =
    generatePdf(context).map { pdfBytes ->
      PdfReader(pdfBytes).use { it.numberOfPages }
    }

  private fun generatePdf(context: RecallSummaryContext) =
    pdfDocumentGenerationService.generatePdf(
      recallSummaryGenerator.generateHtml(context),
      recallImage(HmppsLogo)
    )
}

data class RecallSummaryContext(
  val recall: Recall,
  val prisoner: Prisoner,
  val lastReleasePrisonName: String,
  val currentPrisonName: String,
  val assessor: UserDetails,
  val recallNotificationTotalNumberOfPages: Int? = null
)
