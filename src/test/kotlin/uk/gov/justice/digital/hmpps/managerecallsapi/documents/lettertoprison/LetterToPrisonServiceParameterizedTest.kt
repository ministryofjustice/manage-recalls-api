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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallImage.HmppsLogo
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.time.OffsetDateTime
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("ReactiveStreamsUnusedPublisher")
internal class LetterToPrisonServiceParameterizedTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val letterToPrisonContextFactory = mockk<LetterToPrisonContextFactory>()
  private val documentService = mockk<DocumentService>()
  private val recallRepository = mockk<RecallRepository>()
  private val letterToPrisonCustodyOfficeGenerator = mockk<LetterToPrisonCustodyOfficeGenerator>()
  private val letterToPrisonGovernorGenerator = mockk<LetterToPrisonGovernorGenerator>()
  private val letterToPrisonConfirmationGenerator = mockk<LetterToPrisonConfirmationGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()

  private val underTest = LetterToPrisonService(
    documentService,
    letterToPrisonContextFactory,
    letterToPrisonCustodyOfficeGenerator,
    letterToPrisonGovernorGenerator,
    letterToPrisonConfirmationGenerator,
    pdfDocumentGenerationService,
    pdfDecorator
  )

  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()
  private val nomsNumber = randomNoms()

  @Suppress("unused")
  private fun letterToPrisonParameters(): Stream<Arguments> {
    return Stream.of(
      Arguments.of(RecallLength.FOURTEEN_DAYS, "Annex H – Appeal Papers"),
      Arguments.of(RecallLength.TWENTY_EIGHT_DAYS, "Annex I – Appeal Papers")
    )
  }

  @ParameterizedTest
  @MethodSource("letterToPrisonParameters")
  fun `creates a letter to prison for a recall `(recallLength: RecallLength, annexHeaderText: String) {
    val aRecall = Recall(recallId, nomsNumber, ::UserId.random(), OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"), recallLength = recallLength)
    val documentId = ::DocumentId.random()
    val assessor = UserDetails(
      ::UserId.random(),
      FirstName("Mandy"),
      LastName("Pandy"),
      "",
      Email("mandy@pandy.com"),
      PhoneNumber("09876543210"),
      OffsetDateTime.now()
    )
    val context = LetterToPrisonContext(
      aRecall,
      FullName("Billie Badger"),
      PrisonName("A Prison"),
      PrisonName("Prison B"),
      RecallLengthDescription(recallLength),
      assessor
    )
    val custodyOfficeHtml = "Some CO html, honest"
    val generatedHtml = "Some html, honest"
    val pdfWith3Pages = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val mergedBytes = randomString().toByteArray()
    val mergedNumberedBytes = randomString().toByteArray()

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, LETTER_TO_PRISON) } returns null
    every { letterToPrisonContextFactory.createContext(recallId) } returns context
    every { recallRepository.getByRecallId(recallId) } returns aRecall
    every { letterToPrisonCustodyOfficeGenerator.generateHtml(context) } returns custodyOfficeHtml
    every { letterToPrisonGovernorGenerator.generateHtml(context) } returns generatedHtml
    every { letterToPrisonConfirmationGenerator.generateHtml(context) } returns generatedHtml
    every {
      pdfDocumentGenerationService.generatePdf(custodyOfficeHtml, 1.0, 1.0, recallImage(HmppsLogo))
    } returns Mono.just(pdfWith3Pages)
    every {
      pdfDocumentGenerationService.generatePdf(generatedHtml, 1.0, 1.0, recallImage(HmppsLogo))
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
      documentService.storeDocument(recallId, mergedNumberedBytes, LETTER_TO_PRISON, "$LETTER_TO_PRISON.pdf")
    } returns documentId

    val result = underTest.getPdf(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(mergedNumberedBytes))
        verify {
          documentService.storeDocument(
            recallId,
            mergedNumberedBytes,
            LETTER_TO_PRISON,
            "$LETTER_TO_PRISON.pdf"
          )
        }
      }.verifyComplete()
  }
}
