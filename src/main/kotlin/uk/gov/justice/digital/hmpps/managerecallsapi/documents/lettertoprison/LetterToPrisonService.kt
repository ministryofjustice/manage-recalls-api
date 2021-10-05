package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Data.Companion.documentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService

@Service
class LetterToPrisonService(
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Autowired private val letterToPrisonContextFactory: LetterToPrisonContextFactory,
  @Autowired private val letterToPrisonCustodyOfficeGenerator: LetterToPrisonCustodyOfficeGenerator,
  @Autowired private val letterToPrisonGovernorGenerator: LetterToPrisonGovernorGenerator,
  @Autowired private val letterToPrisonConfirmationGenerator: LetterToPrisonConfirmationGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val pdfDecorator: PdfDecorator,
) {

  fun getPdf(recallId: RecallId): Mono<ByteArray> {
    val letterToPrison = recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, LETTER_TO_PRISON)

    return if (letterToPrison == null) {
      createPdf(recallId).map { letterToPrisonBytes ->
        recallDocumentService.uploadAndAddDocumentForRecall(recallId, letterToPrisonBytes, LETTER_TO_PRISON)
        letterToPrisonBytes
      }
    } else {
      Mono.just(letterToPrison)
    }
  }

  private fun createPdf(recallId: RecallId): Mono<ByteArray> {
    val context = letterToPrisonContextFactory.createContext(recallId)

    val docs = mutableListOf<ByteArrayDocumentData>()

    val letterToPrisonCustodyOfficeHtml = letterToPrisonCustodyOfficeGenerator.generateHtml(context)
    var custodyOfficePageCount = 0

    return pdfDocumentGenerationService.generatePdf(letterToPrisonCustodyOfficeHtml, 1.0, 1.0, recallImage(HmppsLogo))
      .map { custodyBytes ->
        custodyOfficePageCount = PdfReader(custodyBytes).numberOfPages
        docs += documentData(custodyBytes)
      }.flatMap {
        val letterToPrisonGovernorHtml = letterToPrisonGovernorGenerator.generateHtml(context)
        pdfDocumentGenerationService.generatePdf(letterToPrisonGovernorHtml, 1.0, 1.0, recallImage(HmppsLogo))
      }.map { governorBytes ->
        docs += documentData(governorBytes)
      }.flatMap {
        val letterToPrisonConfirmationHtml = letterToPrisonConfirmationGenerator.generateHtml(context)
        pdfDocumentGenerationService.generatePdf(letterToPrisonConfirmationHtml)
      }.map { confBytes ->
        docs += documentData(confBytes)
      }.flatMap {
        pdfDocumentGenerationService.mergePdfs(docs)
      }.map { mergedPdfContentBytes ->
        pdfDecorator.numberPagesOnRightWithHeaderAndFooter(
          mergedPdfContentBytes,
          headerText = "Annex H – Appeal Papers",
          firstHeaderPage = custodyOfficePageCount + 1,
          footerText = "OFFICIAL"
        )
      }
  }
}
