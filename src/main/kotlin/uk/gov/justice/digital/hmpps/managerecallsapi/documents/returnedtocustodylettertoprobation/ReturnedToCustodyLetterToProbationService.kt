package uk.gov.justice.digital.hmpps.managerecallsapi.documents.returnedtocustodylettertoprobation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.config.ReturnedToCustodyRecallExpectedException
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Service
class ReturnedToCustodyLetterToProbationService(
  @Autowired private val documentService: DocumentService,
  @Autowired private val returnedToCustodyLetterToProbationContextFactory: ReturnedToCustodyLetterToProbationContextFactory,
  @Autowired private val returnedToCustodyLetterToProbationGenerator: ReturnedToCustodyLetterToProbationGenerator,
  @Autowired private val pdfDocumentGenerationService: PdfDocumentGenerationService,
  @Autowired private val pdfDecorator: PdfDecorator,
  @Autowired private val recallRepository: RecallRepository,
) {

  fun generateAndStorePdf(
    recallId: RecallId,
    currentUserId: UserId,
    fileName: FileName,
    documentDetails: String?
  ): Mono<DocumentId> =
    recallRepository.getByRecallId(recallId).let { recall ->
      if (recall.returnedToCustody == null) throw ReturnedToCustodyRecallExpectedException(recallId)

      returnedToCustodyLetterToProbationContextFactory.createContext(recall, currentUserId).let { context ->
        pdfDocumentGenerationService.generatePdf(
          returnedToCustodyLetterToProbationGenerator.generateHtml(context),
          1.0, 1.0,
          ImageData.recallImage(RecallImage.HmppsLogo)
        ).map { mergedPdfContentBytes ->
          pdfDecorator.numberPagesOnRightWithHeaderAndFooter(
            mergedPdfContentBytes,
            footerText = "OFFICIAL",
            numberOfPagesToSkip = 2
          )
        }.map { letterBytes ->
          documentService.storeDocument(recallId, currentUserId, letterBytes, LETTER_TO_PROBATION, fileName, documentDetails)
        }
      }
    }
}
