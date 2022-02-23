package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindDecisionRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RescindRecordController.RescindRequestRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RescindRecordService
import java.time.LocalDate

class RescindRecordControllerTest {
  private val rescindRecordService = mockk<RescindRecordService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val bearerToken = "BEARER TOKEN"
  private val userId = ::UserId.random()

  private val underTest = RescindRecordController(
    rescindRecordService,
    tokenExtractor
  )

  private val recallId = ::RecallId.random()

  @Test
  fun `create record `() {
    val documentBytes = "a document".toByteArray()
    val fileName = FileName("email.msg")
    val requestDetails = "blah blah"
    val rescindRecordId = ::RescindRecordId.random()

    val request = RescindRequestRequest(requestDetails, LocalDate.now(), documentBytes.encodeToBase64String(), fileName)

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { rescindRecordService.createRecord(recallId, userId, request) } returns Success(rescindRecordId)

    val response = underTest.createRescindRecord(recallId, request, bearerToken)

    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(response.body, equalTo(rescindRecordId))
  }

  @Test
  fun `decide undecided rescind request`() {
    val documentBytes = "a document".toByteArray()
    val fileName = FileName("file.msg")
    val decisionDetails = "blah blah again"
    val rescindRecordId = ::RescindRecordId.random()

    val decisionRequest = RescindDecisionRequest(false, decisionDetails, LocalDate.now(), documentBytes.encodeToBase64String(), fileName)

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { rescindRecordService.makeDecision(recallId, userId, rescindRecordId, decisionRequest) } returns Success(rescindRecordId)

    val response =
      underTest.decideRescindRecord(recallId, rescindRecordId, decisionRequest, bearerToken)

    assertThat(response.statusCode, equalTo(HttpStatus.OK))
    assertThat(response.body, equalTo(rescindRecordId))
  }
}
