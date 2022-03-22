package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.PartBRecordController.PartBRequest
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PartBRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomHistoricalDate
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.MultiFileException
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

  private val details = randomString()
  private val receivedDate = randomHistoricalDate()
  private val partBFileName = FileName(randomString())
  private val partBContent = randomString().encodeToBase64String()
  private val emailFileName = FileName(randomString())
  private val emailContent = randomString().encodeToBase64String()
  private val oasysFileName = FileName(randomString())
  private val oasysContent = randomString().encodeToBase64String()

  @Test
  fun `create partBRecord calls service with recallId, userId from bearer token and request object and returns Id of new object on success`() {
    val partBRecordId = ::PartBRecordId.random()

    val request = partBRequest()

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userId.toString())
    every { partBRecordService.createRecord(recallId, userId, request) } returns Success(partBRecordId)

    val result = underTest.createPartBRecord(recallId, request, bearerToken)

    assertThat(result, equalTo(partBRecordId))
  }

  @Test
  fun `MultiVirusFoundException wrapping list of failures is thrown on Failure from service`() {

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userId.toString())

    val request = partBRequest()
    val failures = listOf<Pair<DocumentCategory, FileName>>()

    every { partBRecordService.createRecord(recallId, userId, request) } returns Failure(failures)

    val ex = assertThrows<MultiFileException> {
      underTest.createPartBRecord(recallId, request, bearerToken)
    }
    assertThat(ex.failures, equalTo(failures))
  }

  private fun partBRequest() = PartBRequest(
    details,
    receivedDate,
    partBFileName,
    partBContent,
    emailFileName,
    emailContent,
    oasysFileName,
    oasysContent,
  )
}
