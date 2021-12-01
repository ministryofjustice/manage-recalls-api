package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Success
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison.LetterToPrisonService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.OffsetDateTime
import java.util.UUID

class DocumentControllerTest {
  private val documentService = mockk<DocumentService>()
  private val tokenExtractor = mockk<TokenExtractor>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val recallNotificationService = mockk<RecallNotificationService>()
  private val dossierService = mockk<DossierService>()
  private val letterToPrisonService = mockk<LetterToPrisonService>()
  private val advertisedBaseUri = "https://api"

  private val underTest = DocumentController(documentService, recallNotificationService, dossierService, letterToPrisonService, tokenExtractor, userDetailsService, advertisedBaseUri)

  private val recallId = ::RecallId.random()
  private val fileName = "fileName"
  private val details = "Document details provided by user"

  @Test
  fun `adds a recall document successfully`() {
    val document = "a document"
    val documentBytes = document.toByteArray()
    val category = PART_A_RECALL_REPORT
    val documentId = ::DocumentId.random()

    val token = TokenExtractor.Token(UUID.randomUUID().toString())
    every { tokenExtractor.getTokenFromHeader(any()) } returns token

    every {
      documentService.scanAndStoreDocument(
        recallId,
        token.userUuid(),
        documentBytes,
        category,
        fileName,
        details
      )
    } returns Success(documentId)

    val request = AddDocumentRequest(category, documentBytes.encodeToBase64String(), fileName, details)
    val response = underTest.addDocument(recallId, request, "Bearer Token")

    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.headers["Location"]?.toList(),
      present(allElements(equalTo("$advertisedBaseUri/recalls/$recallId/documents/$documentId")))
    )
    assertThat(response.body, equalTo(AddDocumentResponse(documentId)))
  }

  @Test
  fun `gets a document`() {
    val recallId1 = ::RecallId.random()
    val documentId = ::DocumentId.random()
    val now = OffsetDateTime.now()
    val createdByUserId = ::UserId.random()
    val aRecallDocument = Document(
      documentId,
      recallId1,
      PART_A_RECALL_REPORT,
      fileName,
      1,
      createdByUserId,
      now,
      details
    )
    val bytes = "Hello".toByteArray()

    val userDetails = mockk<UserDetails>()
    every { userDetailsService.find(createdByUserId) } returns userDetails
    val fullName = FullName("Caseworker One")
    every { userDetails.fullName() } returns fullName
    every { documentService.getDocument(recallId1, documentId) } returns Pair(aRecallDocument, bytes)

    val response = underTest.getRecallDocument(recallId1, documentId)

    assertThat(response.statusCode, equalTo(HttpStatus.OK))
    val expected = GetDocumentResponse(
      documentId,
      aRecallDocument.category,
      content = bytes.encodeToBase64String(),
      fileName,
      1,
      createdByUserId,
      fullName,
      now,
      details
    )
    assertThat(response.body, equalTo(expected))
  }

  @Test
  fun `delete a document`() {
    val documentId = ::DocumentId.random()

    every { documentService.deleteDocument(recallId, documentId) } just Runs

    underTest.deleteDocument(recallId, documentId)

    verify { documentService.deleteDocument(recallId, documentId) }
  }

  @Suppress("ReactiveStreamsUnusedPublisher")
  @Test
  fun `get recall notification returns Recall Notification PDF`() {
    val recallId = ::RecallId.random()
    val expectedPdf = randomString().toByteArray()
    val expectedBase64Pdf = expectedPdf.encodeToBase64String()
    val userId = ::UserId.random()
    val bearerToken = "BEARER TOKEN"

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { recallNotificationService.getPdf(recallId, userId) } returns Mono.just(expectedPdf)

    val result = underTest.getRecallNotification(recallId, bearerToken)

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
    val recallId = ::RecallId.random()
    val expectedPdf = randomString().toByteArray()
    val expectedBase64Pdf = expectedPdf.encodeToBase64String()
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { dossierService.getPdf(recallId, userId) } returns Mono.just(expectedPdf)

    val result = underTest.getDossier(recallId, bearerToken)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body?.content, equalTo(expectedBase64Pdf))
      }
      .verifyComplete()
  }

  @Test
  fun `get letter to prison returns expected PDF`() {
    val recallId = ::RecallId.random()
    val expectedPdf = randomString().toByteArray()
    val expectedBase64Pdf = expectedPdf.encodeToBase64String()
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { letterToPrisonService.getPdf(recallId, userId) } returns Mono.just(expectedPdf)

    val result = underTest.getLetterToPrison(recallId, bearerToken)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body?.content, equalTo(expectedBase64Pdf))
      }
      .verifyComplete()
  }
}
