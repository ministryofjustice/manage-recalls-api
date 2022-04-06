package uk.gov.justice.digital.hmpps.managerecallsapi.documents.returnedtocustodylettertoprobation

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.LocalDeliveryUnit
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.NameFormatCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallLength
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.STANDARD
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PROBATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.RecallDescription
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.BookingNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonLookupService
import java.time.LocalDate
import java.time.OffsetDateTime

class ReturnedToCustodyLetterToProbationContextFactoryTest {
  private val prisonLookupService = mockk<PrisonLookupService>()
  private val documentRepository = mockk<DocumentRepository>()

  val underTest = ReturnedToCustodyLetterToProbationContextFactory(
    prisonLookupService,
    documentRepository
  )
  private val recallId = ::RecallId.random()
  private val createdByUserId = ::UserId.random()
  private val recallLength = RecallLength.TWENTY_EIGHT_DAYS
  private val fixedTermRecall = Recall(
    recallId, NomsNumber("AA1234A"), ::UserId.random(),
    OffsetDateTime.now(), FirstName("Barrie"), null, LastName("Badger"),
    CroNumber("ABC/1234A"), LocalDate.of(1999, 12, 1),
    bookingNumber = BookingNumber("AB1234"),
    confirmedRecallType = FIXED,
    differentNomsNumber = false,
    currentPrison = PrisonId("WIM"),
    lastReleasePrison = PrisonId("BOB"),
    licenceNameCategory = NameFormatCategory.FIRST_LAST,
    recallLength = recallLength,
    probationInfo = ProbationInfo(FullName("Paul Probation"), PhoneNumber("0123"), Email("m@m.com"), LocalDeliveryUnit.CENTRAL_AUDIT_TEAM, FullName("Mr ACO")),
    returnedToCustodyRecord = ReturnedToCustodyRecord(OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), createdByUserId, OffsetDateTime.now())
  )

  @Test
  fun `create NotInCustodyLetterToProbationContext with all required data uses todays date for first version`() {
    val currentPrisonName = PrisonName("WIM Prison")

    every { prisonLookupService.getPrisonName(fixedTermRecall.currentPrison!!) } returns currentPrisonName
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, LETTER_TO_PROBATION, 1) } returns null

    val result = underTest.createContext(fixedTermRecall, createdByUserId)

    assertThat(
      result,
      equalTo(
        ReturnedToCustodyLetterToProbationContext(
          RecallDescription(FIXED, recallLength),
          fixedTermRecall.bookingNumber!!,
          fixedTermRecall.nomsNumber,
          false,
          fixedTermRecall.nomsNumber,
          FullName("Paul Probation"),
          FullName("Barrie Badger"),
          currentPrisonName,
          LocalDate.now().minusDays(1),
          LocalDate.now(),
          FullName("Mr ACO"),
          null
        )
      )
    )
  }

  @Test
  fun `create NotInCustodyLetterToProbationContext with all required data uses created date of first version of letter`() {
    val currentPrisonName = PrisonName("WIM Prison")
    val document = mockk<Document>()

    every { prisonLookupService.getPrisonName(fixedTermRecall.currentPrison!!) } returns currentPrisonName
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, LETTER_TO_PROBATION, 1) } returns document
    every { document.createdDateTime } returns OffsetDateTime.now().minusDays(5)

    val result = underTest.createContext(fixedTermRecall, createdByUserId)

    assertThat(
      result,
      equalTo(
        ReturnedToCustodyLetterToProbationContext(
          RecallDescription(FIXED, recallLength),
          fixedTermRecall.bookingNumber!!,
          fixedTermRecall.nomsNumber,
          false,
          fixedTermRecall.nomsNumber,
          FullName("Paul Probation"),
          FullName("Barrie Badger"),
          currentPrisonName,
          LocalDate.now().minusDays(1),
          LocalDate.now().minusDays(5),
          FullName("Mr ACO"),
          null
        )
      )
    )
  }

  @Test
  fun `create NotInCustodyLetterToProbationContext with all required data for standard recall`() {
    val standardRecall = fixedTermRecall.copy(confirmedRecallType = STANDARD, recallLength = null, partBDueDate = LocalDate.now().plusDays(20))
    val currentPrisonName = PrisonName("WIM Prison")

    every { prisonLookupService.getPrisonName(standardRecall.currentPrison!!) } returns currentPrisonName
    every { documentRepository.findByRecallIdAndCategoryAndVersion(recallId.value, LETTER_TO_PROBATION, 1) } returns null

    val result = underTest.createContext(standardRecall, createdByUserId)

    assertThat(
      result,
      equalTo(
        ReturnedToCustodyLetterToProbationContext(
          RecallDescription(STANDARD, null),
          standardRecall.bookingNumber!!,
          standardRecall.nomsNumber,
          false,
          standardRecall.nomsNumber,
          FullName("Paul Probation"),
          FullName("Barrie Badger"),
          currentPrisonName,
          LocalDate.now().minusDays(1),
          LocalDate.now(),
          FullName("Mr ACO"),
          LocalDate.now().plusDays(20)
        )
      )
    )
  }
}
