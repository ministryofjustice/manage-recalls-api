package uk.gov.justice.digital.hmpps.managerecallsapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ClassPathDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.StringDocumentDetail
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId

@Service
class LetterToProbationService(
  @Autowired private val pdfDocumentGenerator: PdfDocumentGenerator,
  @Autowired private val letterToProbationGenerator: LetterToProbationGenerator,
) {

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val populatedHtml = letterToProbationGenerator.generateHtml()

    val details = listOf(
      StringDocumentDetail("index.html", populatedHtml),
      ClassPathDocumentDetail("letter-to-probation-logo.png", "/templates/images/letter-to-probation-logo.png")
    )

    return pdfDocumentGenerator.makePdf(details)
  }
}
