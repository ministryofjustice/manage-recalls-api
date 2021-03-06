package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.lowagie.text.pdf.PdfReader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.Data.Companion.documentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class LetterToPrisonService(
  @Autowired private val documentService: DocumentService,
  @Autowired private val letterToPrisonContextFactory: LetterToPrisonContextFactory,
  @Autowired private val letterToPrisonCustodyOfficeGenerator: LetterToPrisonCustodyOfficeGenerator,
  @Autowired private val letterToPrisonGovernorGenerator: LetterToPrisonGovernorGenerator,
  @Autowired private val letterToPrisonConfirmationGenerator: LetterToPrisonConfirmationGenerator,
  @Autowired private val letterToPrisonStandardPartsGenerator: LetterToPrisonStandardPartsGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val pdfDecorator: PdfDecorator,
) {

  fun generateAndStorePdf(
    recallId: RecallId,
    currentUserId: UserId,
    fileName: FileName,
    documentDetails: String?
  ): Mono<DocumentId> {
    val context = letterToPrisonContextFactory.createContext(recallId, currentUserId)
    var letterToPrisonCustodyOfficePageCount = 0

    val documentGeneratorList: Array<() -> Mono<ByteArray>> = listOf(
      generateLetterToPrisonCustodyOffice(context),
      generateLetterToPrisonGovernor(context),
    ).plus(getRecallTypeSpecificDocumentGenerators(context))
      .toTypedArray()

    val documentGenerators: Flux<() -> Mono<ByteArray>> = Flux.just(*documentGeneratorList)
    return documentGenerators
      .flatMapSequential { it() }
      .map { documentData(it) }
      .collectList()
      .map { letterToPrisonDocuments ->
        letterToPrisonDocuments.also {
          val letterToPrisonCustodyOfficeBytes = it.first().byteArray
          letterToPrisonCustodyOfficePageCount = PdfReader(letterToPrisonCustodyOfficeBytes).numberOfPages
        }
      }
      .flatMap { letterToPrisonDocuments ->
        pdfDocumentGenerationService.mergePdfs(letterToPrisonDocuments)
      }.map { mergedPdfContentBytes ->
        pdfDecorator.numberPagesOnRightWithHeaderAndFooter(
          mergedPdfContentBytes,
          headerText = context.recallDescription.letterToPrisonAppealsPapersHeading(),
          firstHeaderPage = letterToPrisonCustodyOfficePageCount + 1,
          footerText = "OFFICIAL"
        )
      }.map { letterToPrisonBytes ->
        documentService.storeDocument(recallId, currentUserId, letterToPrisonBytes, LETTER_TO_PRISON, fileName, documentDetails)
      }
  }

  private fun getRecallTypeSpecificDocumentGenerators(context: LetterToPrisonContext): List<() -> Mono<ByteArray>> =
    if (context.recallDescription.isFixedTermRecall()) {
      listOf(generateLetterToPrisonConfirmation(context))
    } else {
      listOf(
        generateLetterToPrisonStandardPart1(context),
        generateLetterToPrisonStandardPart2(context),
        generateLetterToPrisonStandardPart3(context),
      )
    }

  private fun generateLetterToPrisonStandardPart1(context: LetterToPrisonContext): () -> Mono<ByteArray> =
    {
      val letterToPrisonConfirmationHtml = letterToPrisonStandardPartsGenerator.generatePart1Html(context.getStandardPartsContext())
      pdfDocumentGenerationService.generatePdf(letterToPrisonConfirmationHtml)
    }

  private fun generateLetterToPrisonStandardPart2(context: LetterToPrisonContext): () -> Mono<ByteArray> =
    {
      val letterToPrisonConfirmationHtml = letterToPrisonStandardPartsGenerator.generatePart2Html(context.getStandardPartsContext())
      pdfDocumentGenerationService.generatePdf(letterToPrisonConfirmationHtml)
    }

  private fun generateLetterToPrisonStandardPart3(context: LetterToPrisonContext): () -> Mono<ByteArray> =
    {
      val letterToPrisonConfirmationHtml = letterToPrisonStandardPartsGenerator.generatePart3Html(context.getStandardPartsContext())
      pdfDocumentGenerationService.generatePdf(letterToPrisonConfirmationHtml)
    }

  private fun generateLetterToPrisonConfirmation(context: LetterToPrisonContext): () -> Mono<ByteArray> =
    {
      val letterToPrisonConfirmationHtml = letterToPrisonConfirmationGenerator.generateHtml(context.getConfirmationContext())
      pdfDocumentGenerationService.generatePdf(letterToPrisonConfirmationHtml)
    }

  private fun generateLetterToPrisonGovernor(context: LetterToPrisonContext): () -> Mono<ByteArray> =
    {
      val letterToPrisonGovernorHtml = letterToPrisonGovernorGenerator.generateHtml(context.getGovernorContext())
      pdfDocumentGenerationService.generatePdf(letterToPrisonGovernorHtml, 1.0, 0.8, recallImage(HmppsLogo))
    }

  private fun generateLetterToPrisonCustodyOffice(context: LetterToPrisonContext): () -> Mono<ByteArray> =
    {
      val letterToPrisonCustodyOfficeHtml = letterToPrisonCustodyOfficeGenerator.generateHtml(context.getCustodyContext())
      pdfDocumentGenerationService.generatePdf(letterToPrisonCustodyOfficeHtml, 1.0, 0.8, recallImage(HmppsLogo))
    }
}
