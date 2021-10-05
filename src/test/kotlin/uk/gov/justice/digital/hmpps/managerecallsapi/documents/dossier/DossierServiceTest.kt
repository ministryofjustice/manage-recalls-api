package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.byteArrayDocumentDataFor
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.RecallInformationLeaflet
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.onlyContainsInOrder
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService

@Suppress("ReactiveStreamsUnusedPublisher")
internal class DossierServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val tableOfContentsService = mockk<TableOfContentsService>()
  private val recallDocumentService = mockk<RecallDocumentService>()
  private val reasonsForRecallService = mockk<ReasonsForRecallService>()
  private val pdfDecorator = mockk<PdfDecorator>()
  private val dossierContextFactory = mockk<DossierContextFactory>()
  private val dossierContext = mockk<DossierContext>()

  private val underTest = DossierService(
    pdfDocumentGenerationService,
    recallDocumentService,
    reasonsForRecallService,
    pdfDecorator,
    tableOfContentsService,
    dossierContextFactory
  )

  @Test
  fun `get dossier returns table of contents, recall information leaflet, license, part A, revocation order and reasons for recall as dossier when all available for recall`() {
    val recallId = ::RecallId.random()
    val licenseContentBytes = randomString().toByteArray()
    val partARecallReportContentBytes = randomString().toByteArray()
    val revocationOrderContentBytes = randomString().toByteArray()
    val reasonsForRecallContentBytes = randomString().toByteArray()
    val mergedBytes = randomString().toByteArray()
    val tableOfContentBytes = randomString().toByteArray()
    val numberedMergedBytes = randomString().toByteArray()
    val documentsToMergeSlot = slot<List<ByteArrayDocumentData>>()

    every { dossierContextFactory.createContext(recallId) } returns dossierContext
    every { recallDocumentService.getDocumentContentWithCategory(recallId, LICENCE) } returns licenseContentBytes
    every { recallDocumentService.getDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT) } returns partARecallReportContentBytes
    every { recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER) } returns revocationOrderContentBytes
    every { reasonsForRecallService.createPdf(dossierContext) } returns Mono.just(reasonsForRecallContentBytes)
    every { tableOfContentsService.createPdf(dossierContext, any()) } returns Mono.just(tableOfContentBytes) // assert on documents
    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every { pdfDecorator.numberPages(mergedBytes, 1) } returns numberedMergedBytes

    val dossier = underTest.getDossier(recallId).block()!!

    assertArrayEquals(numberedMergedBytes, dossier)
    assertThat(
      documentsToMergeSlot.captured,
      onlyContainsInOrder(
        listOf(
          byteArrayDocumentDataFor(tableOfContentBytes),
          byteArrayDocumentDataFor(RecallInformationLeaflet.byteArray()),
          byteArrayDocumentDataFor(licenseContentBytes),
          byteArrayDocumentDataFor(partARecallReportContentBytes),
          byteArrayDocumentDataFor(revocationOrderContentBytes),
          byteArrayDocumentDataFor(reasonsForRecallContentBytes)
        )
      )
    )
  }

  // TODO: PUD-575 test/handling when any input doc is not available
}
