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
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.time.LocalDate
import java.time.OffsetDateTime
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
  private val revocationOrderId = UUID.randomUUID()
  private val recallRequest = BookRecallRequest(nomsNumber)

  @Test
  fun `book recall returns request with id`() {
    val recall = recallRequest.toRecall()

    every { recallRepository.save(any()) } returns recall

    val results = underTest.bookRecall(recallRequest)

    val expected = RecallResponse(
      recallId = recall.recallId(),
      nomsNumber = nomsNumber,
      documents = emptyList(),
      reasonsForRecall = emptyList()
    )
    assertThat(results.body, equalTo(expected))
  }

  @Test
  fun `gets all recalls`() {
    val recall = Recall(recallId, nomsNumber)

    every { recallRepository.findAll() } returns listOf(recall)

    val results = underTest.findAll()

    assertThat(
      results,
      equalTo(listOf(RecallResponse(recallId, nomsNumber, emptyList(), reasonsForRecall = emptyList())))
    )
  }

  @Test
  fun `gets a recall`() {
    val document = RecallDocument(
      id = UUID.randomUUID(),
      recallId = UUID.randomUUID(),
      category = RecallDocumentCategory.PART_A_RECALL_REPORT
    )
    val recallEmailReceivedDateTime = OffsetDateTime.now()
    val lastReleaseDate = LocalDate.now()
    val recall = Recall(
      recallId = recallId,
      nomsNumber = nomsNumber,
      revocationOrderId = revocationOrderId,
      documents = setOf(document),
      agreeWithRecallRecommendation = true,
      lastReleasePrison = "prison",
      lastReleaseDate = lastReleaseDate,
      recallEmailReceivedDateTime = recallEmailReceivedDateTime
    )
    every { recallRepository.getByRecallId(recallId) } returns recall

    val result = underTest.getRecall(recallId)

    val expected = RecallResponse(
      recallId = recallId,
      nomsNumber = nomsNumber,
      documents = listOf(
        ApiRecallDocument(
          documentId = document.id,
          category = document.category
        )
      ),
      revocationOrderId = revocationOrderId,
      agreeWithRecallRecommendation = true,
      lastReleasePrison = "prison",
      lastReleaseDate = lastReleaseDate,
      recallEmailReceivedDateTime = recallEmailReceivedDateTime,
      reasonsForRecall = emptyList()
    )
    assertThat(result, equalTo(expected))
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
  fun `'add document' responds with BAD_REQUEST if target recall is not found`() {
    val document = "a document"
    val documentBytes = document.toByteArray()
    val category = RecallDocumentCategory.PART_A_RECALL_REPORT
    val cause = Throwable()
    val recallNotFoundError = RecallNotFoundException("boom!", cause)

    every { recallDocumentService.addDocumentToRecall(recallId, documentBytes, category) } throws recallNotFoundError

    val request = AddDocumentRequest(
      category = category.toString(),
      fileContent = Base64.getEncoder().encodeToString(documentBytes)
    )

    val exception = assertThrows<ResponseStatusException> { underTest.addDocument(recallId, request) }

    assertThat(exception.status, equalTo(HttpStatus.BAD_REQUEST))
    assertThat(exception.reason, equalTo(recallNotFoundError.message))
    assertThat(exception.cause, equalTo(recallNotFoundError))
  }

  @Test
  fun `'get document' responds with NOT_FOUND if recall is not found`() {
    val cause = Throwable()
    val recallNotFoundError = RecallNotFoundException("boom!", cause)

    every { recallDocumentService.getDocument(any(), any()) } throws recallNotFoundError

    val exception = assertThrows<ResponseStatusException> {
      underTest.getRecallDocument(::RecallId.random(), UUID.randomUUID())
    }

    assertThat(exception.status, equalTo(HttpStatus.NOT_FOUND))
    assertThat(exception.reason, equalTo(recallNotFoundError.message))
    assertThat(exception.cause, equalTo(recallNotFoundError))
  }

  @Test
  fun `'get document' responds with NOT_FOUND if document is not found`() {
    val cause = Throwable()
    val recallDocumentNotFoundError = RecallDocumentNotFoundException("boom!", cause)

    every { recallDocumentService.getDocument(any(), any()) } throws recallDocumentNotFoundError

    val exception = assertThrows<ResponseStatusException> {
      underTest.getRecallDocument(::RecallId.random(), UUID.randomUUID())
    }

    assertThat(exception.status, equalTo(HttpStatus.NOT_FOUND))
    assertThat(exception.reason, equalTo(recallDocumentNotFoundError.message))
    assertThat(exception.cause, equalTo(recallDocumentNotFoundError))
  }

  @Test
  fun `gets a document`() {
    val recallId1 = ::RecallId.random()
    val documentId = UUID.randomUUID()
    val aRecallDocument = RecallDocument(
      documentId,
      recallId1.value,
      RecallDocumentCategory.PART_A_RECALL_REPORT
    )
    val bytes = "Hello".toByteArray()

    every { recallDocumentService.getDocument(recallId1, documentId) } returns Pair(aRecallDocument, bytes)

    val response = underTest.getRecallDocument(recallId1, documentId)

    assertThat(response.statusCode, equalTo(HttpStatus.OK))
    val expected = GetDocumentResponse(
      documentId,
      aRecallDocument.category,
      content = Base64.getEncoder().encodeToString(bytes)
    )
    assertThat(response.body, equalTo(expected))
  }
}
