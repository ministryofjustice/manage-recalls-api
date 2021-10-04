package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class LetterToPrisonServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val letterToPrisonContextFactory = mockk<LetterToPrisonContextFactory>()
  private val recallDocumentService = mockk<RecallDocumentService>()
  private val recallRepository = mockk<RecallRepository>()
  private val letterToPrisonCustodyOfficeGenerator = mockk<LetterToPrisonCustodyOfficeGenerator>()
  private val letterToPrisonGovernorGenerator = mockk<LetterToPrisonGovernorGenerator>()
  private val letterToPrisonConfirmationGenerator = mockk<LetterToPrisonConfirmationGenerator>()
  private val pdfDecorator = mockk<PdfDecorator>()

  private val underTest = LetterToPrisonService(
    recallDocumentService,
    letterToPrisonContextFactory,
    letterToPrisonCustodyOfficeGenerator,
    letterToPrisonGovernorGenerator,
    letterToPrisonConfirmationGenerator,
    pdfDocumentGenerationService,
    pdfDecorator
  )

  // TODO: will be good to agree as the dev team whether we prefer this model or local vals per test fun
  private val recallId = ::RecallId.random()
  private val expectedBytes = randomString().toByteArray()
  private val nomsNumber = randomNoms()

  @Test
  fun `returns a letter to prison for a recall if one exists already`() {
    every { recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, LETTER_TO_PRISON) } returns expectedBytes

    val result = underTest.getPdf(recallId)

    verify(exactly = 0) { recallRepository.getByRecallId(any()) }
    verify(exactly = 0) { letterToPrisonContextFactory.createContext(any()) }
    verify(exactly = 0) { letterToPrisonCustodyOfficeGenerator.generateHtml(any()) }
    verify(exactly = 0) { letterToPrisonGovernorGenerator.generateHtml(any()) }
    verify(exactly = 0) { letterToPrisonConfirmationGenerator.generateHtml(any()) }
    verify(exactly = 0) { pdfDocumentGenerationService.generatePdf(any()) }
    verify(exactly = 0) { pdfDocumentGenerationService.mergePdfs(any()) }
    verify(exactly = 0) { pdfDecorator.numberPagesOnRightWithHeaderAndFooter(any(), any(), any(), any(), any(), any(), any()) }

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(expectedBytes))
      }.verifyComplete()
  }

  @Test
  fun `creates a letter to prison for a recall `() {
    val aRecall = Recall(recallId, nomsNumber)
    val documentId = UUID.randomUUID()
    val assessor = UserDetails(::UserId.random(), FirstName("Mandy"), LastName("Pandy"), "", Email("mandy@pandy.com"), PhoneNumber("09876543210"))
    val context = LetterToPrisonContext(aRecall, Prisoner(), PrisonName("A Prison"), PrisonName("Prison B"), assessor)
    val custodyOfficeHtml = "Some CO html, honest"
    val generatedHtml = "Some html, honest"
    val pdfWith3Pages = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val mergedBytes = randomString().toByteArray()
    val mergedNumberedBytes = randomString().toByteArray()

    every { recallDocumentService.getDocumentContentWithCategoryIfExists(recallId, LETTER_TO_PRISON) } returns null
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
    every { pdfDecorator.numberPagesOnRightWithHeaderAndFooter(mergedBytes, headerText = "Annex H – Appeal Papers", firstHeaderPage = 4, footerText = "OFFICIAL") } returns mergedNumberedBytes
    every {
      recallDocumentService.uploadAndAddDocumentForRecall(recallId, mergedNumberedBytes, LETTER_TO_PRISON)
    } returns documentId

    val result = underTest.getPdf(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(mergedNumberedBytes))
        verify { recallDocumentService.uploadAndAddDocumentForRecall(recallId, mergedNumberedBytes, LETTER_TO_PRISON) }
      }.verifyComplete()
  }
}
