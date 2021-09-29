package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData.Companion.recallImage
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

class RecallSummaryServiceTest {
  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val recallSummaryGenerator = mockk<RecallSummaryGenerator>()
  private val recallSummaryContextFactory = mockk<RecallSummaryContextFactory>()

  private val underTest = RecallSummaryService(
    pdfDocumentGenerationService,
    recallSummaryGenerator,
    recallSummaryContextFactory
  )

  @Test
  fun `generates the recall summary PDF with required information`() {
    val recallId = ::RecallId.random()
    val recallSummaryHtmlWithoutPageCount = "generated Html without page count"
    val recallSummaryHtmlWithPageCount = "generated Html with page count"
    val pdfWith3Pages = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()

    val recallSummaryContext = RecallSummaryContext(mockk(), mockk(), "don't care", "don't care", UserDetails(::UserId.random(), FirstName("Bob"), LastName("Badger"), "", Email("b@bob.com"), PhoneNumber("0987")))
    val recallSummaryContextWithPageCountSlot = slot<RecallSummaryContext>()

    every { recallSummaryContextFactory.createRecallSummaryContext(recallId) } returns Mono.just(recallSummaryContext)
    every { recallSummaryGenerator.generateHtml(recallSummaryContext) } returns recallSummaryHtmlWithoutPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithoutPageCount, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)
    every { recallSummaryGenerator.generateHtml(capture(recallSummaryContextWithPageCountSlot)) } returns recallSummaryHtmlWithPageCount
    every { pdfDocumentGenerationService.generatePdf(recallSummaryHtmlWithPageCount, recallImage(HmppsLogo)) } returns Mono.just(pdfWith3Pages)

    val result = underTest.getPdf(recallId).block()!!

    assertThat(recallSummaryContextWithPageCountSlot.captured.recallNotificationTotalNumberOfPages, equalTo(5))
    assertArrayEquals(pdfWith3Pages, result)
  }
}
