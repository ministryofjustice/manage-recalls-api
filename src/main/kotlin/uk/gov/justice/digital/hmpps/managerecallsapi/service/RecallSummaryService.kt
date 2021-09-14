package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.StringDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId

@Service
class RecallSummaryService(
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator,
  @Autowired private val templateEngine: SpringTemplateEngine,
) {

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val ctx = Context()
    val populatedHtml = templateEngine.process("recall-summary", ctx)

    val details = listOf(
      StringDocumentDetail("index.html", populatedHtml),
      ClassPathDocumentDetail("recall-summary-logo.png", "/templates/images/recall-summary-logo.png")
    )

    return pdfDocumentGenerator.makePdf(details)
  }
}
