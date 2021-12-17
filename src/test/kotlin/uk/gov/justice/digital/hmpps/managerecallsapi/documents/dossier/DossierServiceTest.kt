package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.ByteArrayDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.byteArrayDocumentDataFor
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.FixedTermRecallInformationLeafletEnglish
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.FixedTermRecallInformationLeafletWelsh
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
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
  fun `generate dossier returns table of contents, English recall information leaflet, license, part A, revocation order and reasons for recall as dossier when all available for recall`() {
    testDossierGetPdf(false)
  }

  @Test
  fun `generate dossier to include Welsh returns table of contents, both recall information leaflets, license, part A, revocation order and reasons for recall as dossier when all available for recall`() {
    testDossierGetPdf(true)
  }

  private fun testDossierGetPdf(includeWelshLeaflet: Boolean) {
    val recallId = ::RecallId.random()
    val createdByUserId = ::UserId.random()
    val licenseContentBytes = randomString().toByteArray()
    val partARecallReportContentBytes = randomString().toByteArray()
    val revocationOrderContentBytes = randomString().toByteArray()
    val reasonsForRecallContentBytes = randomString().toByteArray()
    val mergedBytes = randomString().toByteArray()
    val tableOfContentBytes = randomString().toByteArray()
    val numberedMergedBytes = randomString().toByteArray()
    val documentsToMergeSlot = slot<List<ByteArrayDocumentData>>()

    every { dossierContext.includeWelsh() } returns includeWelshLeaflet
    every { dossierContextFactory.createContext(recallId) } returns dossierContext
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, LICENCE) } returns licenseContentBytes
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT) } returns partARecallReportContentBytes
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER) } returns revocationOrderContentBytes
    every { reasonsForRecallService.getOrGeneratePdf(dossierContext, createdByUserId) } returns Mono.just(reasonsForRecallContentBytes)
    every { tableOfContentsService.generatePdf(dossierContext, any()) } returns Mono.just(tableOfContentBytes) // assert on documents
    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every { pdfDecorator.numberPages(mergedBytes, 1) } returns numberedMergedBytes
    every { documentService.storeDocument(recallId, createdByUserId, numberedMergedBytes, DOSSIER, "DOSSIER.pdf") } returns ::DocumentId.random()

    underTest.generateAndStorePdf(recallId, createdByUserId, null).block()!!

    val expectedDocumentsToMerge = mutableListOf(
      byteArrayDocumentDataFor(tableOfContentBytes),
      byteArrayDocumentDataFor(FixedTermRecallInformationLeafletEnglish.byteArray())
    )

    if (includeWelshLeaflet) {
      expectedDocumentsToMerge.add(byteArrayDocumentDataFor(FixedTermRecallInformationLeafletWelsh.byteArray()))
    }

    expectedDocumentsToMerge.addAll(
      listOf(
        byteArrayDocumentDataFor(licenseContentBytes),
        byteArrayDocumentDataFor(partARecallReportContentBytes),
        byteArrayDocumentDataFor(revocationOrderContentBytes),
        byteArrayDocumentDataFor(reasonsForRecallContentBytes)
      )
    )

    assertThat(documentsToMergeSlot.captured, onlyContainsInOrder(expectedDocumentsToMerge))
  }
}
