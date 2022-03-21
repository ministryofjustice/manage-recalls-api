package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("ReactiveStreamsUnusedPublisher")
internal class LetterToPrisonServiceParameterizedTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val letterToPrisonContextFactory = mockk<LetterToPrisonContextFactory>()
  private val documentService = mockk<DocumentService>()
  private val letterToPrisonCustodyOfficeGenerator = mockk<LetterToPrisonCustodyOfficeGenerator>()
  private val letterToPrisonGovernorGenerator = mockk<LetterToPrisonGovernorGenerator>()
  private val letterToPrisonConfirmationGenerator = mockk<LetterToPrisonConfirmationGenerator>()
  private val letterToPrisonStandardPartsGenerator = mockk<LetterToPrisonStandardPartsGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()

  private val underTest = LetterToPrisonService(
    documentService,
    letterToPrisonContextFactory,
    letterToPrisonCustodyOfficeGenerator,
    letterToPrisonGovernorGenerator,
    letterToPrisonConfirmationGenerator,
    letterToPrisonStandardPartsGenerator,
    pdfDocumentGenerationService,
    pdfDecorator
  )

  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()

  @Suppress("unused")
  private fun letterToPrisonParameters(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(RecallLength.FOURTEEN_DAYS, "Annex H – Appeal Papers"),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, "Annex I – Appeal Papers")
    )
  }

  @ParameterizedTest
  @MethodSource("letterToPrisonParameters")
  fun `generates a letter to prison for a recall `(recallLength: RecallLength, annexHeaderText: String) {
    val documentId = ::DocumentId.random()
    val context = mockk<LetterToPrisonContext>()
    val custodyOfficeHtml = "Some CO html, honest"
    val generatedHtml = "Some html, honest"
    val pdfWith3Pages = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val mergedBytes = randomString().toByteArray()
    val mergedNumberedBytes = randomString().toByteArray()
    val createdByUserId = ::UserId.random()
    val fileName = FileName("LETTER_TO_PRISON.pdf")

    every { letterToPrisonContextFactory.createContext(recallId, createdByUserId) } returns context
    every { context.recallDescription } returns RecallDescription(RecallType.FIXED, recallLength)
    every { context.getCustodyContext() } returns mockk()
    every { context.getGovernorContext() } returns mockk()
    every { context.getConfirmationContext() } returns mockk()
    every { letterToPrisonCustodyOfficeGenerator.generateHtml(context.getCustodyContext()) } returns custodyOfficeHtml
    every { letterToPrisonGovernorGenerator.generateHtml(context.getGovernorContext()) } returns generatedHtml
    every { letterToPrisonConfirmationGenerator.generateHtml(context.getConfirmationContext()) } returns generatedHtml
    every {
      pdfDocumentGenerationService.generatePdf(custodyOfficeHtml, 1.0, 0.8, recallImage(HmppsLogo))
    } returns Mono.just(pdfWith3Pages)
    every {
      pdfDocumentGenerationService.generatePdf(generatedHtml, 1.0, 0.8, recallImage(HmppsLogo))
    } returns Mono.just(expectedBytes)
    every { pdfDocumentGenerationService.generatePdf(generatedHtml) } returns Mono.just(expectedBytes)
    every { pdfDocumentGenerationService.mergePdfs(any()) } returns Mono.just(mergedBytes)
    every {
      pdfDecorator.numberPagesOnRightWithHeaderAndFooter(
        mergedBytes,
        headerText = annexHeaderText,
        firstHeaderPage = 4,
        footerText = "OFFICIAL"
      )
    } returns mergedNumberedBytes
    every {
      documentService.storeDocument(
        recallId,
        createdByUserId,
        mergedNumberedBytes,
        LETTER_TO_PRISON,
        fileName
      )
    } returns documentId

    val result = underTest.generateAndStorePdf(recallId, createdByUserId, fileName, null)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(documentId))
        verify {
          documentService.storeDocument(
            recallId,
            createdByUserId,
            mergedNumberedBytes,
            LETTER_TO_PRISON,
            fileName
          )
        }
      }.verifyComplete()
  }
}
