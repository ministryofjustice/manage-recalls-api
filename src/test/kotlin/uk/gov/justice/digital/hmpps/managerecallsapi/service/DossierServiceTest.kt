package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.InputStreamDocumentData
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDecorator
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.matchers.onlyContainsInOrder
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallClassPathResource.RecallInformationLeaflet

@Suppress("ReactiveStreamsUnusedPublisher")
internal class DossierServiceTest {

  private val pdfDocumentGenerationService = mockk<PdfDocumentGenerationService>()
  private val recallDocumentService = mockk<RecallDocumentService>()
  private val pdfDecorator = mockk<PdfDecorator>()

  private val underTest = DossierService(pdfDocumentGenerationService, recallDocumentService, pdfDecorator)

  @Test
  fun `get dossier returns part A, license and revocation order as dossier when all present for recall`() {
    val recallId = ::RecallId.random()
    val licenseContent = randomString()
    val partARecallReportContent = randomString()
    val revocationOrderContent = randomString()
    val mergedBytes = randomString().toByteArray()
    val numberedMergedBytes = randomString().toByteArray()
    val documentsToMergeSlot = slot<List<InputStreamDocumentData>>()

    every { recallDocumentService.getDocumentContentWithCategory(recallId, LICENCE) } returns licenseContent.toByteArray()
    every { recallDocumentService.getDocumentContentWithCategory(recallId, PART_A_RECALL_REPORT) } returns partARecallReportContent.toByteArray()
    every { recallDocumentService.getDocumentContentWithCategory(recallId, REVOCATION_ORDER) } returns revocationOrderContent.toByteArray()
    every { pdfDocumentGenerationService.mergePdfs(capture(documentsToMergeSlot)) } returns Mono.just(mergedBytes)
    every { pdfDecorator.numberPages(mergedBytes) } returns numberedMergedBytes

    val dossier = underTest.getDossier(recallId).block()!!

    assertThat(dossier, equalTo(numberedMergedBytes))
    assertThat(
      documentsToMergeSlot.captured,
      onlyContainsInOrder(
        listOf(
          inputStreamDocumentDataFor(RecallInformationLeaflet),
          inputStreamDocumentDataFor(licenseContent),
          inputStreamDocumentDataFor(partARecallReportContent),
          inputStreamDocumentDataFor(revocationOrderContent)
        )
      )
    )
  }

  // TODO: PUD-575 test/handling when any input doc is not available
}
