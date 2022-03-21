package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomHistoricalDate
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PartBRecordService

class PartBRecordControllerTest {

  private val partBRecordService = mockk<PartBRecordService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val bearerToken = "BEARER TOKEN"
  private val userId = ::UserId.random()

  private val underTest = PartBRecordController(
    partBRecordService,
    tokenExtractor
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `create partBRecord calls service with recallId, userId from bearer token and request object`() {
    val partBRecordId = ::PartBRecordId.random()

    val request = PartBRecordController.PartBRequest(
      randomString(),
      randomHistoricalDate(),
      FileName(randomString()),
      randomString().encodeToBase64String(),
      FileName(randomString()),
      randomString().encodeToBase64String(),
      FileName(randomString()),
      randomString().encodeToBase64String(),
    )

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userId.toString())
    every { partBRecordService.createRecord(recallId, userId, request) } returns Success(partBRecordId)

    val result = underTest.createPartBRecord(recallId, request, bearerToken)

    assertThat(result, equalTo(partBRecordId))
  }

  // TODO PUD-1605 Multiple error handling for virus found
}
