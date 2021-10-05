package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.SearchRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ImageData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.search.Prisoner
import uk.gov.justice.digital.hmpps.managerecallsapi.search.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallImage.HmppsLogo

@Suppress("ReactiveStreamsUnusedPublisher")
internal class TableOfContentsServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val tableOfContentsGenerator = mockk<TableOfContentsGenerator>()
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val prisonerOffenderSearchClient = mockk<PrisonerOffenderSearchClient>()

  private val underTest = TableOfContentsService(pdfDocumentGenerationService, tableOfContentsGenerator, recallRepository, prisonLookupService, prisonerOffenderSearchClient)

  @Test
  fun `get table of contents for supplied documents`() {
    val documentStream1 = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()
    val documentStream2 = ClassPathResource("/document/3_pages_unnumbered.pdf").file.readBytes()

    val recallId = ::RecallId.random()
    val prisoner = Prisoner()
    val nomsNumber = NomsNumber("AB1234C")
    val recall = Recall(recallId, nomsNumber, currentPrison = PrisonId("ABC"))
    val someHtml = "Some content"

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonerOffenderSearchClient.prisonerSearch(SearchRequest(nomsNumber)) } returns Mono.just(listOf(prisoner))
    every { prisonLookupService.getPrisonName(PrisonId("ABC")) } returns PrisonName("A Prison")
    every {
      tableOfContentsGenerator.generateHtml(
        TableOfContentsContext(
          recall,
          prisoner,
          PrisonName("A Prison"),
          listOf(Document("Document 1", 1), Document("Document 2", 4))
        )
      )
    } returns someHtml
    val tocBytes = "Some bytes".toByteArray()
    every {
      pdfDocumentGenerationService.generatePdf(
        someHtml,
        ImageData.recallImage(HmppsLogo)
      )
    } returns Mono.just(tocBytes)

    val tableOfContents = underTest.createPdf(
      recallId,
      mapOf(
        "Document 1" to ByteArrayDocumentData(documentStream1),
        "Document 2" to ByteArrayDocumentData(documentStream2)
      )
    ).block()!!

    assertThat(tableOfContents, equalTo(tocBytes))
  }
}
