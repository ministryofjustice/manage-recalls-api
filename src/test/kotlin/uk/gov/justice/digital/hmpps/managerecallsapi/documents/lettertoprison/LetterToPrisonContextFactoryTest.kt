package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.MappaLevel
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomBookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomHistoricalDate
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import java.time.LocalDate
import java.time.OffsetDateTime

class LetterToPrisonContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val documentRepository = mockk<DocumentRepository>()

  val underTest = LetterToPrisonContextFactory(recallRepository, prisonLookupService, documentRepository)

  @Test
  fun `create LetterToPrisonContext with all required data uses todays date for first version`() {

    val recallId = ::RecallId.random()
    val createdByUserId = ::UserId.random()
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    val bookingNumber = randomBookingNumber()
    val nomsNumber = randomNoms()
    val recall = Recall(
      recallId, nomsNumber, ::UserId.random(),
      OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"),
      CroNumber("ABC/1234A"), LocalDate.of(1999, 12, 1),
      confirmedRecallType = FIXED,
      currentPrison = PrisonId("WIM"),
      lastReleasePrison = PrisonId("BOB"),
      licenceNameCategory = NameFormatCategory.FIRST_LAST,
      recallLength = recallLength,
      bookingNumber = bookingNumber,
      lastReleaseDate = LocalDate.now(),
      differentNomsNumber = false,
      additionalLicenceConditions = false,
      contraband = false,
      vulnerabilityDiversity = false,
      mappaLevel = MappaLevel.NA
    )
    val currentPrisonName = PrisonName("WIM Prison")
    val lastReleasePrisonName = PrisonName("Bobbins Prison")
    val recallDescription = RecallDescription(FIXED, recallLength)

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(recall.currentPrison!!) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(recall.lastReleasePrison!!) } returns lastReleasePrisonName
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, LETTER_TO_PRISON, 1) } returns null

    val result = underTest.createContext(recallId, createdByUserId)

    assertThat(
      result,
      equalTo(
        LetterToPrisonContext(
          FullName("Barrie Badger"),
          currentPrisonName,
          lastReleasePrisonName,
          recallDescription,
          bookingNumber,
          LocalDate.now(),
          LocalDate.now(),
          nomsNumber,
          false,
          nomsNumber,
          false,
          null,
          false,
          null,
          false,
          null,
          MappaLevel.NA
        )
      )
    )
  }
  @Test
  fun `create LetterToPrisonContext with all required data uses created date of first version of letter`() {

    val recallId = ::RecallId.random()
    val createdByUserId = ::UserId.random()
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    val lastReleaseDate = randomHistoricalDate()
    val bookingNumber = randomBookingNumber()
    val nomsNumber = randomNoms()
    val recall = Recall(
      recallId, nomsNumber, ::UserId.random(),
      OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"),
      CroNumber("ABC/1234A"), LocalDate.of(1999, 12, 1),
      confirmedRecallType = FIXED,
      currentPrison = PrisonId("WIM"),
      lastReleasePrison = PrisonId("BOB"),
      licenceNameCategory = NameFormatCategory.FIRST_LAST,
      recallLength = recallLength,
      bookingNumber = bookingNumber,
      lastReleaseDate = lastReleaseDate,
      differentNomsNumber = false,
      additionalLicenceConditions = false,
      contraband = false,
      vulnerabilityDiversity = false,
      mappaLevel = MappaLevel.NA
    )
    val currentPrisonName = PrisonName("WIM Prison")
    val lastReleasePrisonName = PrisonName("Bobbins Prison")
    val recallDescription = RecallDescription(FIXED, recallLength)
    val document = mockk<Document>()
    val originalCreatedDateTime = OffsetDateTime.now().minusDays(4)

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(recall.currentPrison!!) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(recall.lastReleasePrison!!) } returns lastReleasePrisonName
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, LETTER_TO_PRISON, 1) } returns document
    every { document.createdDateTime } returns originalCreatedDateTime

    val result = underTest.createContext(recallId, createdByUserId)

    assertThat(
      result,
      equalTo(
        LetterToPrisonContext(
          FullName("Barrie Badger"),
          currentPrisonName,
          lastReleasePrisonName,
          recallDescription,
          bookingNumber,
          lastReleaseDate,
          originalCreatedDateTime.toLocalDate(),
          nomsNumber,
          false,
          nomsNumber,
          false,
          null,
          false,
          null,
          false,
          null,
          MappaLevel.NA
        )
      )
    )
  }
}
