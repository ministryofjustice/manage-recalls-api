package uk.gov.justice.digital.hmpps.managerecallsapi.service

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.PdfDocumentGenerator
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import java.util.UUID

@Suppress("ReactiveStreamsUnusedPublisher")
internal class DossierServiceTest {

  private val revocationOrderService = mockk<RevocationOrderService>()
  private val pdfDocumentGenerator = mockk<PdfDocumentGenerator>()
  private val recallDocumentService = mockk<RecallDocumentService>()

  private val underTest = DossierService(
    revocationOrderService,
    pdfDocumentGenerator,
    recallDocumentService
  )

  @Test
  fun `get dossier returns part A, license and revocation order as dossier when all present for recall`() {
    val recallId = ::RecallId.random()
    val licenseBytes = randomString().toByteArray()
    val partARecallReportBytes = randomString().toByteArray()
    val revocationOrderBytes = randomString().toByteArray()
    val mergedBytes = randomString().toByteArray()

    val licensePair = Pair(recallDocument(recallId.value, LICENCE), licenseBytes)
    val partaRecallReportPair = Pair(recallDocument(recallId.value, PART_A_RECALL_REPORT), partARecallReportBytes)

    every { recallDocumentService.getDocumentWithCategory(recallId, LICENCE) } returns licensePair
    every { recallDocumentService.getDocumentWithCategory(recallId, PART_A_RECALL_REPORT) } returns partaRecallReportPair
    every { revocationOrderService.getRevocationOrder(recallId) } returns Mono.just(revocationOrderBytes)
    every { pdfDocumentGenerator.mergePdfs(any()) } returns Mono.just(mergedBytes) // TODO: test that the argument list has 3 entries; the correct entries; but note likely to change call to decouple from Gotenberg

    val result = underTest.getDossier(recallId)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it, equalTo(mergedBytes))
      }
      .verifyComplete()
  }

  // TODO: test/handling when any input doc is not available.

  private fun recallDocument(recallId: UUID, category: RecallDocumentCategory) =
    RecallDocument(UUID.randomUUID(), recallId, category, randomString())
}
