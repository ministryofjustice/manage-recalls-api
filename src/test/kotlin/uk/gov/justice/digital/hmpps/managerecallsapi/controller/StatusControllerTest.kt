package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ReturnedToCustodyRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import java.time.LocalDate
import java.time.OffsetDateTime

class StatusControllerTest {
  private val recallService = mockk<RecallService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val createdByUserId = ::UserId.random()
  private val firstName = FirstName("Barrie")
  private val lastName = LastName("Badger")
  private val croNumber = CroNumber("ABC/1234A")
  private val now = OffsetDateTime.now()
  private val bearerToken = "Bearer header.payload"
  private val userUuid = ::UserId.random()

  private val recall = Recall(
    recallId, nomsNumber, createdByUserId, now, firstName, null, lastName,
    croNumber, LocalDate.of(1999, 12, 1)
  )

  private val underTest = StatusController(recallService, tokenExtractor)

  @Test
  fun `update returned to custody`() {
    val returnedToCustodyDateTime = OffsetDateTime.now().minusHours(3)
    val returnedToCustodyNotificationDateTime = OffsetDateTime.now().minusMinutes(10)
    val returnedToCustodyRequest = ReturnedToCustodyRequest(returnedToCustodyDateTime, returnedToCustodyNotificationDateTime)
    val returnedToCustodyRecord = ReturnedToCustodyRecord(
      returnedToCustodyDateTime,
      returnedToCustodyNotificationDateTime,
      OffsetDateTime.now(),
      userUuid
    )
    val updatedRecall = recall.copy(returnedToCustody = returnedToCustodyRecord, dossierTargetDate = LocalDate.now().plusDays(1))

    every { recallService.manuallyReturnedToCustody(recallId, returnedToCustodyDateTime, returnedToCustodyNotificationDateTime, userUuid) } returns updatedRecall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userUuid.toString())

    underTest.returnedToCustody(recallId, returnedToCustodyRequest, bearerToken)

    verify { tokenExtractor.getTokenFromHeader(bearerToken) }
    verify { recallService.manuallyReturnedToCustody(recallId, returnedToCustodyDateTime, returnedToCustodyNotificationDateTime, userUuid) }
  }

  @Test
  fun `stop recall`() {
    val stopReason = StopReason.values().filter { it.validForStopCall }.random()
    val request = StopRecallRequest(stopReason)

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userUuid.toString())
    every { recallService.stopRecall(recallId, request, userUuid) } returns recall

    underTest.stopRecall(recallId, request, bearerToken)

    verify { tokenExtractor.getTokenFromHeader(bearerToken) }
    verify { recallService.stopRecall(recallId, request, userUuid) }
  }
}
