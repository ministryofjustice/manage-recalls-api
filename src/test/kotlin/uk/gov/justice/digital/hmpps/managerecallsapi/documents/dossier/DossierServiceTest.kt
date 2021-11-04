package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.byteArrayDocumentDataFor
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.RecallInformationLeaflet
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.onlyContainsInOrder
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService

@Suppress("ReactiveStreamsUnusedPublisher")
internal class DossierServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val tableOfContentsService = mockk<TableOfContentsService>()
  private val documentService = mockk<DocumentService>()
  private val reasonsForRecallService = mockk<ReasonsForRecallService>()
  private val pdfDecorator = mockk<PdfDecorator>()
  private val dossierContextFactory = mockk<DossierContextFactory>()
  private val dossierContext = mockk<DossierContext>()

  private val underTest = DossierService(
    pdfDocumentGenerationService,
    documentService,
    reasonsForRecallService,
    pdfDecorator,
    tableOfContentsService,
    dossierContextFactory
  )

  @Test
  fun `get dossier returns table of contents, recall information leaflet, license, part A, revocation order and reasons for recall as dossier when all available for recall and dossier doesnt already exist`() {
    val recallId = ::RecallId.random()
    val licenseContentBytes = randomString().toByteArray()
    val partARecallReportContentBytes = randomString().toByteArray()
    val revocationOrderContentBytes = randomString().toByteArray()
    val reasonsForRecallContentBytes = randomString().toByteArray()
    val mergedBytes = randomString().toByteArray()
    val tableOfContentBytes = randomString().toByteArray()
    val numberedMergedBytes = randomString().toByteArray()
    val documentsToMergeSlot = slot<List<ByteArrayDocumentData>>()

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, DOSSIER) } returns null
    every { dossierContextFactory.createContext(recallId) } returns dossierContext
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, LICENCE) } returns licenseContentBytes
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT) } returns partARecallReportContentBytes
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER) } returns revocationOrderContentBytes
    every { reasonsForRecallService.getDocument(dossierContext) } returns Mono.just(reasonsForRecallContentBytes)
    every { tableOfContentsService.createPdf(dossierContext, any()) } returns Mono.just(tableOfContentBytes) // assert on documents
    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every { pdfDecorator.numberPages(mergedBytes, 1) } returns numberedMergedBytes
    every { documentService.storeDocument(recallId, numberedMergedBytes, DOSSIER, "DOSSIER.pdf") } returns ::DocumentId.random()

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

  @Test
  fun `get dossier returns dossier if it already exists`() {
    val recallId = ::RecallId.random()
    val documentBytes = randomString().toByteArray()

    every { documentService.getLatestVersionedDocumentContentWithCategoryIfExists(recallId, DOSSIER) } returns documentBytes

    val dossier = underTest.getDossier(recallId).block()!!

    assertArrayEquals(documentBytes, dossier)

    verify { dossierContextFactory wasNot Called }
    verify { reasonsForRecallService wasNot Called }
    verify { tableOfContentsService wasNot Called }
    verify { pdfDocumentGenerationService wasNot Called }
    verify { pdfDecorator wasNot Called }
    verify(exactly = 0) { documentService.storeDocument(any(), any(), any(), any()) }
  }

  // TODO: PUD-575 test/handling when any input doc is not available
}
