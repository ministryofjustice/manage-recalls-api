package uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallLengthDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.time.OffsetDateTime

class LetterToPrisonContextFactoryTest {
  private val recallRepository = mockk<RecallRepository>()
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val documentRepository = mockk<DocumentRepository>()

  val underTest = LetterToPrisonContextFactory(recallRepository, prisonLookupService, userDetailsService, documentRepository)

  @Test
  fun `create LetterToPrisonContext with all required data uses todays date for first version`() {

    val recallId = ::RecallId.random()
    val createdByUserId = ::UserId.random()
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    val recall = Recall(
      recallId, NomsNumber("AA1234A"), ::UserId.random(),
      OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"),
      CroNumber("ABC/1234A"), LocalDate.of(1999, 12, 1),
      recallLength = recallLength,
      lastReleasePrison = PrisonId("BOB"),
      currentPrison = PrisonId("WIM")
    )
    val createdByUserDetails = mockk<UserDetails>()
    val currentPrisonName = PrisonName("WIM Prison")
    val lastReleasePrisonName = PrisonName("Bobbins Prison")
    val recallLengthDescription = RecallLengthDescription(recallLength)

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(recall.currentPrison!!) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(recall.lastReleasePrison!!) } returns lastReleasePrisonName
    every { userDetailsService.get(createdByUserId) } returns createdByUserDetails
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, LETTER_TO_PRISON, 1) } returns null

    val result = underTest.createContext(recallId, createdByUserId)

    assertThat(
      result,
      equalTo(
        LetterToPrisonContext(
          recall,
          FullName("Barrie Badger"),
          currentPrisonName,
          lastReleasePrisonName,
          recallLengthDescription,
          createdByUserDetails,
          LocalDate.now()
        )
      )
    )
  }
  @Test
  fun `create LetterToPrisonContext with all required data uses created date of first version of letter`() {

    val recallId = ::RecallId.random()
    val createdByUserId = ::UserId.random()
    val recallLength = RecallLength.TWENTY_EIGHT_DAYS
    val recall = Recall(
      recallId, NomsNumber("AA1234A"), ::UserId.random(),
      OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"),
      CroNumber("ABC/1234A"), LocalDate.of(1999, 12, 1),
      recallLength = recallLength,
      lastReleasePrison = PrisonId("BOB"),
      currentPrison = PrisonId("WIM")
    )
    val createdByUserDetails = mockk<UserDetails>()
    val currentPrisonName = PrisonName("WIM Prison")
    val lastReleasePrisonName = PrisonName("Bobbins Prison")
    val recallLengthDescription = RecallLengthDescription(recallLength)
    val document = mockk<Document>()
    val originalCreatedDateTime = OffsetDateTime.now().minusDays(4)

    every { recallRepository.getByRecallId(recallId) } returns recall
    every { prisonLookupService.getPrisonName(recall.currentPrison!!) } returns currentPrisonName
    every { prisonLookupService.getPrisonName(recall.lastReleasePrison!!) } returns lastReleasePrisonName
    every { userDetailsService.get(createdByUserId) } returns createdByUserDetails
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, LETTER_TO_PRISON, 1) } returns document
    every { document.createdDateTime } returns originalCreatedDateTime

    val result = underTest.createContext(recallId, createdByUserId)

    assertThat(
      result,
      equalTo(
        LetterToPrisonContext(
          recall,
          FullName("Barrie Badger"),
          currentPrisonName,
          lastReleasePrisonName,
          recallLengthDescription,
          createdByUserDetails,
          originalCreatedDateTime.toLocalDate()
        )
      )
    )
  }
}
