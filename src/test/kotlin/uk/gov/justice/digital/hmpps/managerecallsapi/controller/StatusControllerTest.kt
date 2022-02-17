package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.BankHolidayService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class StatusControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val bankHolidayService = mockk<BankHolidayService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val fixedClock = Clock.fixed(Instant.parse("2022-02-04T11:14:20.00Z"), ZoneId.of("UTC"))

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val createdByUserId = ::UserId.random()
  private val firstName = FirstName("Barrie")
  private val lastName = LastName("Badger")
  private val croNumber = CroNumber("ABC/1234A")
  private val now = OffsetDateTime.now()

  private val recall = Recall(
    recallId, nomsNumber, createdByUserId, now, firstName, null, lastName,
    croNumber, LocalDate.of(1999, 12, 1)
  )

  private val underTest = StatusController(recallRepository, bankHolidayService, tokenExtractor, fixedClock)

  @Test
  fun `update returned to custody`() {
    val bearerToken = "Bearer header.payload"
    val userUuid = ::UserId.random()

    val returnedToCustodyDateTime = OffsetDateTime.now().minusHours(3)
    val returnedToCustodyNotificationDateTime = OffsetDateTime.now().minusMinutes(10)
    val returnedToCustodyRequest = ReturnedToCustodyRequest(returnedToCustodyDateTime, returnedToCustodyNotificationDateTime)
    val returnedToCustodyRecord = ReturnedToCustodyRecord(returnedToCustodyDateTime, returnedToCustodyNotificationDateTime, userUuid, OffsetDateTime.now(fixedClock))
    val updatedRecall = recall.copy(returnedToCustody = returnedToCustodyRecord, dossierTargetDate = LocalDate.now().plusDays(1))

    every { recallRepository.getByRecallId(any()) } returns recall
    every { recallRepository.save(updatedRecall, userUuid) } returns updatedRecall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userUuid.toString())
    every { bankHolidayService.nextWorkingDate(returnedToCustodyDateTime.toLocalDate()) } returns LocalDate.now().plusDays(1)

    val results = underTest.returnedToCustody(recallId, returnedToCustodyRequest, bearerToken)

    assertThat(results.statusCode, equalTo(HttpStatus.OK))
  }
}
