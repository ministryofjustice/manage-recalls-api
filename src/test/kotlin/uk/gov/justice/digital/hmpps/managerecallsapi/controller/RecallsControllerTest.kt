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
import uk.gov.justice.digital.hmpps.managerecallsapi.component.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DossierService
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
  private val dossierService = mockk<DossierService>()

  private val underTest =
    RecallsController(recallRepository, revocationOrderService, recallDocumentService, dossierService, advertisedBaseUri)

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val revocationOrderId = UUID.randomUUID()
  private val recallRequest = BookRecallRequest(nomsNumber)
  private val fileName = "fileName"

  @Test
  fun `book recall returns request with id`() {
    val recall = recallRequest.toRecall()

    every { recallRepository.save(any()) } returns recall

    val results = underTest.bookRecall(recallRequest)

    val expected = RecallResponse(recallId = recall.recallId(), nomsNumber = nomsNumber)
    assertThat(results.body, equalTo(expected))
  }

  @Test
  fun `gets all recalls`() {
    val recall = Recall(recallId, nomsNumber)

    every { recallRepository.findAll() } returns listOf(recall)

    val results = underTest.findAll()

    assertThat(results, equalTo(listOf(RecallResponse(recallId, nomsNumber))))
  }

  @Test
  fun `gets a recall`() {
    val document = RecallDocument(
      id = UUID.randomUUID(),
      recallId = UUID.randomUUID(),
      category = RecallDocumentCategory.PART_A_RECALL_REPORT,
      fileName = fileName
    )
    val recallEmailReceivedDateTime = OffsetDateTime.now()
    val lastReleaseDate = LocalDate.now()
    val recall = Recall(
      recallId = recallId,
      nomsNumber = nomsNumber,
      revocationOrderId = revocationOrderId,
      documents = setOf(document),
      lastReleasePrison = "BEL",
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
          category = document.category,
          fileName = fileName
        )
      ),
      revocationOrderId = revocationOrderId,
      lastReleasePrison = "BEL",
      lastReleaseDate = lastReleaseDate,
      recallEmailReceivedDateTime = recallEmailReceivedDateTime,
    )
    assertThat(result, equalTo(expected))
  }

  @Suppress("ReactiveStreamsUnusedPublisher")
  @Test
  fun `get revocation order returns RevocationOrder PDF`() {
    val recall = recallRequest.toRecall()
    val expectedPdf = randomString().toByteArray()
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

  @Suppress("ReactiveStreamsUnusedPublisher")
  @Test
  fun `get dossier returns expected PDF`() {
    val recall = recallRequest.toRecall()
    val expectedPdf = randomString().toByteArray()
    val expectedBase64Pdf = Base64.getEncoder().encodeToString(expectedPdf)

    every { dossierService.getDossier(recall.recallId()) } returns Mono.just(expectedPdf)

    val result = underTest.getDossier(recall.recallId())

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

    every { recallDocumentService.addDocumentToRecall(recallId, documentBytes, category, fileName) } returns documentId

    val request = AddDocumentRequest(category, Base64.getEncoder().encodeToString(documentBytes), fileName)
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
    val recallNotFoundError = RecallNotFoundException(recallId)

    every { recallDocumentService.addDocumentToRecall(recallId, documentBytes, category, fileName) } throws recallNotFoundError

    val request = AddDocumentRequest(category, Base64.getEncoder().encodeToString(documentBytes), fileName)

    val exception = assertThrows<ResponseStatusException> { underTest.addDocument(recallId, request) }

    assertThat(exception.status, equalTo(HttpStatus.BAD_REQUEST))
    assertThat(exception.reason, equalTo(recallNotFoundError.message))
    assertThat(exception.cause, equalTo(recallNotFoundError))
  }

  @Test
  fun `gets a document`() {
    val recallId1 = ::RecallId.random()
    val documentId = UUID.randomUUID()
    val aRecallDocument = RecallDocument(
      documentId,
      recallId1.value,
      RecallDocumentCategory.PART_A_RECALL_REPORT,
      fileName
    )
    val bytes = "Hello".toByteArray()

    every { recallDocumentService.getDocument(recallId1, documentId) } returns Pair(aRecallDocument, bytes)

    val response = underTest.getRecallDocument(recallId1, documentId)

    assertThat(response.statusCode, equalTo(HttpStatus.OK))
    val expected = GetDocumentResponse(
      documentId,
      aRecallDocument.category,
      content = Base64.getEncoder().encodeToString(bytes),
      fileName
    )
    assertThat(response.body, equalTo(expected))
  }
}
