package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundError
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.util.Base64
import java.util.UUID

class RecallsControllerTest {
  private val advertisedBaseUri = "https://api"

  private val recallRepository = mockk<RecallRepository>()
  private val revocationOrderService = mockk<RevocationOrderService>()
  private val recallDocumentService = mockk<RecallDocumentService>()

  private val underTest =
    RecallsController(recallRepository, revocationOrderService, recallDocumentService, advertisedBaseUri)

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val revocationOrderDocS3Key = UUID.randomUUID()
  private val recallRequest = BookRecallRequest(nomsNumber)

  @Test
  fun `book recall returns request with id`() {
    val recall = recallRequest.toRecall()
    every { recallRepository.save(any()) } returns recall

    val results = underTest.bookRecall(recallRequest)

    assertThat(results.body, equalTo(RecallResponse(recall.recallId(), nomsNumber, null)))
  }

  @Test
  fun `gets all recalls`() {
    val recall = Recall(recallId, nomsNumber)
    every { recallRepository.findAll() } returns listOf(recall)

    val results = underTest.findAll()

    assertThat(results, equalTo(listOf(RecallResponse(recallId, nomsNumber, null))))
  }

  @Test
  fun `gets a recall`() {
    val recall = Recall(recallId, nomsNumber, revocationOrderDocS3Key)
    every { recallRepository.getByRecallId(recallId) } returns recall

    val results = underTest.getRecall(recallId)

    assertThat(results, equalTo(RecallResponse(recallId, nomsNumber, revocationOrderDocS3Key)))
  }

  @Test
  fun `book a recall`() {
    val recall = recallRequest.toRecall()
    val expectedPdf = "Some pdf".toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

    every { revocationOrderService.getRevocationOrder(recall.recallId()) } returns Mono.just(expectedPdf)

    val result = underTest.getRevocationOrder(recall.recallId())

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body?.content, equalTo(expectedBase64Pdf))
      }
      .verifyComplete()
  }

  @Test
  fun `adds a recall document successfully`() {
    val document = "a document"
    val documentBytes = document.toByteArray()
    val category = RecallDocumentCategory.PART_A_RECALL_REPORT
    val documentId = UUID.randomUUID()

    every { recallDocumentService.addDocumentToRecall(recallId, documentBytes, category) } returns documentId

    val request = AddDocumentRequest(
      category = category.toString(),
      fileContent = Base64.getEncoder().encodeToString(documentBytes)
    )
    val response = underTest.addDocument(recallId, request)

    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.headers["Location"]?.toList(),
      present(allElements(equalTo("$advertisedBaseUri/recalls/$recallId/documents/$documentId")))
    )
    assertThat(response.body, equalTo(AddDocumentResponse(documentId)))
  }

  @Test
  fun `responds with 400 if target recall for document addition does not exist`() {
    val document = "a document"
    val documentBytes = document.toByteArray()
    val category = RecallDocumentCategory.PART_A_RECALL_REPORT
    val cause = Throwable()
    val recallNotFoundError = RecallNotFoundError("boom!", cause)

    every { recallDocumentService.addDocumentToRecall(recallId, documentBytes, category) } throws
      recallNotFoundError

    val request = AddDocumentRequest(
      category = category.toString(),
      fileContent = Base64.getEncoder().encodeToString(documentBytes)
    )

    val exception = assertThrows<ResponseStatusException> { underTest.addDocument(recallId, request) }

    assertThat(exception.status, equalTo(HttpStatus.BAD_REQUEST))
    assertThat(exception.reason, equalTo(recallNotFoundError.message))
    assertThat(exception.cause, equalTo(recallNotFoundError))
  }
}
