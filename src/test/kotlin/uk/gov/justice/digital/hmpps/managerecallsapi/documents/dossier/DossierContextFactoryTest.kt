package uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.FOURTEEN_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength.TWENTY_EIGHT_DAYS
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
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
import java.util.stream.Stream
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
      confirmedRecallType = FIXED,
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
      currentPrison = currentPrison,
      recommendedRecallType = STANDARD
    )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.isWelsh(currentPrison) } returns currentPrisonIsWelsh
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, DocumentCategory.DOSSIER) } returns null

    val result = underTest.createContext(recallId)

    assertThat(result, equalTo(DossierContext(recall, currentPrisonName, currentPrisonIsWelsh, 1)))
  }

  private fun parameters(): Stream<Arguments> =
    Stream.of(
      Arguments.of(FIXED, TWENTY_EIGHT_DAYS, "28 Day FTR 12 months & over"),
      Arguments.of(FIXED, FOURTEEN_DAYS, "14 Day FTR under 12 months"),
      Arguments.of(STANDARD, null, "Standard 255c recall review"),
    )

  @ParameterizedTest(name = "TableOfContentsContext for {0} recall includes recallLength as {1} with correct tocDescription")
  @MethodSource("parameters")
  fun `get TableOfContentsContext`(recallType: RecallType, recallLength: RecallLength?, tocDescription: String) {
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
      bookingNumber = BookingNumber("A1234"),
      currentPrison = currentPrison,
      licenceNameCategory = NameFormatCategory.FIRST_LAST,
      recallLength = recallLength,
      recommendedRecallType = recallType
    )

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(currentPrison) } returns currentPrisonName
    every { prisonLookupService.isWelsh(currentPrison) } returns currentPrisonIsWelsh
    every { documentRepository.findLatestVersionedDocumentByRecallIdAndCategory(recallId, DocumentCategory.DOSSIER) } returns null

    val result = underTest.createContext(recallId).getTableOfContentsContext()

    assertThat(
      result,
      equalTo(
        TableOfContentsContext(
          recall.prisonerNameOnLicence(),
          RecallDescription(recallType, recallLength),
          currentPrisonName,
          BookingNumber("A1234"),
          1
        )
      )
    )

    assertThat(result.recallDescription.tableOfContentsDescription(), equalTo(tocDescription))
  }
}
