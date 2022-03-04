package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.StandardTermRecallInformationLeafletEnglish
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.RecallClassPathResource.StandardTermRecallInformationLeafletWelsh
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.onlyContainsInOrder
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

  private fun parameterArrays(): Stream<Arguments>? =
    Stream.of(
      Arguments.of(false, FIXED),
      Arguments.of(true, FIXED),
      Arguments.of(false, STANDARD),
      Arguments.of(true, STANDARD)
    )

  @ParameterizedTest(name = "generate dossier with TOC, English {1} recall information leaflet, welsh leaflet ? {0}, license, part a, revocation order & reasons for recall ")
  @MethodSource("parameterArrays")
  fun testDossierGetPdf(includeWelshLeaflet: Boolean, recallType: RecallType) {
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
    val fileName = FileName("DOSSIER.pdf")

    every { dossierContext.includeWelsh() } returns includeWelshLeaflet
    every { dossierContext.recallType } returns recallType
    every { dossierContextFactory.createContext(recallId) } returns dossierContext
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, LICENCE) } returns licenseContentBytes
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT) } returns partARecallReportContentBytes
    every { documentService.getLatestVersionedDocumentContentWithCategory(recallId, REVOCATION_ORDER) } returns revocationOrderContentBytes
    every { reasonsForRecallService.getOrGeneratePdf(dossierContext, createdByUserId) } returns Mono.just(reasonsForRecallContentBytes)
    every { tableOfContentsService.generatePdf(dossierContext, any()) } returns Mono.just(tableOfContentBytes) // assert on documents
    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every { pdfDecorator.numberPages(mergedBytes, 1) } returns numberedMergedBytes
    every { documentService.storeDocument(recallId, createdByUserId, numberedMergedBytes, DOSSIER, fileName) } returns ::DocumentId.random()

    underTest.generateAndStorePdf(recallId, createdByUserId, fileName, null).block()!!

    val expectedDocumentsToMerge = mutableListOf(
      byteArrayDocumentDataFor(tableOfContentBytes),
      if (recallType == FIXED) {
        byteArrayDocumentDataFor(FixedTermRecallInformationLeafletEnglish.byteArray())
      } else
        byteArrayDocumentDataFor(StandardTermRecallInformationLeafletEnglish.byteArray())
    )

    if (includeWelshLeaflet) {
      if (recallType == FIXED) {
        expectedDocumentsToMerge.add(byteArrayDocumentDataFor(FixedTermRecallInformationLeafletWelsh.byteArray()))
      } else
        expectedDocumentsToMerge.add(byteArrayDocumentDataFor(StandardTermRecallInformationLeafletWelsh.byteArray()))
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
