package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.random.Random

class DossierContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val documentRepository = mockk<DocumentRepository>()

  private val underTest = DossierContextFactory(
    recallRepository, prisonLookupService, documentRepository
  )

  @Test
  fun `create DossierContextFactory with required details when a dossier already exists`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val currentPrison = PrisonId("AAA")
    val currentPrisonName = PrisonName("Current Prison Name")
    val currentPrisonIsWelsh = Random.nextBoolean()
    val document = mockk<Document>()

    val recall = Recall(
      recallId,
      nomsNumber,
      ::UserId.random(),
      OffsetDateTime.now(),
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      currentPrison = currentPrison
    )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.isWelsh(currentPrison) } returns currentPrisonIsWelsh
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, DocumentCategory.DOSSIER) } returns document
    every { document.version } returns 2

    val result = underTest.createContext(recallId)

    assertThat(result, equalTo(DossierContext(recall, currentPrisonName, currentPrisonIsWelsh, 3)))
  }
  @Test
  fun `create DossierContextFactory with required details when no dossier exists`() {
    val recallId = ::RecallId.random()
    val nomsNumber = NomsNumber("nomsNumber")
    val currentPrison = PrisonId("AAA")
    val currentPrisonName = PrisonName("Current Prison Name")
    val currentPrisonIsWelsh = Random.nextBoolean()

    val recall = Recall(
      recallId,
      nomsNumber,
      ::UserId.random(),
      OffsetDateTime.now(),
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      CroNumber("ABC/1234A"),
      LocalDate.of(1999, 12, 1),
      currentPrison = currentPrison
    )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.isWelsh(currentPrison) } returns currentPrisonIsWelsh
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, DocumentCategory.DOSSIER) } returns null

    val result = underTest.createContext(recallId)

    assertThat(result, equalTo(DossierContext(recall, currentPrisonName, currentPrisonIsWelsh, 1)))
  }
}
