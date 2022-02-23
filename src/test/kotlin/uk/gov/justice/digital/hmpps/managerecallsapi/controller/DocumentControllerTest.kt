package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Success
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.config.WrongDocumentTypeException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.DOSSIER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LETTER_TO_PRISON
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REASONS_FOR_RECALL
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.RECALL_NOTIFICATION
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.REVOCATION_ORDER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.ReasonsForRecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison.LetterToPrisonService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RevocationOrderService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.OffsetDateTime
import java.util.UUID

class DocumentControllerTest {
  private val documentService = mockk<DocumentService>()
  private val tokenExtractor = mockk<TokenExtractor>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val recallNotificationService = mockk<RecallNotificationService>()
  private val revocationOrderService = mockk<RevocationOrderService>()
  private val reasonsForRecallService = mockk<ReasonsForRecallService>()
  private val dossierService = mockk<DossierService>()
  private val letterToPrisonService = mockk<LetterToPrisonService>()
  private val advertisedBaseUri = "https://api"

  private val underTest = DocumentController(
    documentService,
    recallNotificationService,
    dossierService,
    letterToPrisonService,
    revocationOrderService,
    reasonsForRecallService,
    tokenExtractor,
    userDetailsService,
    advertisedBaseUri
  )

  private val recallId = ::RecallId.random()
  private val fileName = FileName("filename")
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

    val request = UploadDocumentRequest(category, documentBytes.encodeToBase64String(), fileName, details)
    val response = underTest.uploadDocument(recallId, request, "Bearer Token")

    assertThat(response.statusCode, equalTo(HttpStatus.CREATED))
    assertThat(
      response.headers["Location"]?.toList(),
      present(allElements(equalTo("$advertisedBaseUri/recalls/$recallId/documents/$documentId")))
    )
    assertThat(response.body, equalTo(NewDocumentResponse(documentId)))
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
      details,
      now,
      createdByUserId
    )
    val bytes = "Hello".toByteArray()

    val userDetails = mockk<UserDetails>()
    every { userDetailsService.get(createdByUserId) } returns userDetails
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
      details,
      fullName,
      now
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

  @Test
  fun `get all documents for a given category for a recall`() {
    val recallId = ::RecallId.random()
    val category = RECALL_NOTIFICATION
    val now = OffsetDateTime.now()
    val createdByUserId = ::UserId.random()
    val document = Document(::DocumentId.random(), recallId, category, FileName("file.pdf"), 1, null, now, createdByUserId)
    val userDetails = mockk<UserDetails>()

    every { documentService.getAllDocumentsByCategory(recallId, category) } returns listOf(document)
    every { userDetailsService.get(createdByUserId) } returns userDetails
    every { userDetails.fullName() } returns FullName("Andy Newton")

    val result = underTest.getRecallDocumentsByCategory(recallId, category)

    assertThat(result.body, equalTo(listOf(Api.RecallDocument(document.id(), category, FileName("file.pdf"), 1, null, now, FullName("Andy Newton")))))
  }

  @Test
  fun `upload document throws exception for non-uploaded document categories`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())

    DocumentCategory.values().filter { !it.uploaded }.forEach {
      assertThrows<WrongDocumentTypeException> {
        underTest.uploadDocument(recallId, UploadDocumentRequest(it, "blah, blah, blah", FileName("filename")), bearerToken)
      }
    }
  }

  @Test
  fun `generate document throws exception for uploaded document categories`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())

    DocumentCategory.values().filter { it.uploaded }.forEach {
      assertThrows<WrongDocumentTypeException> {
        underTest.generateDocument(recallId, GenerateDocumentRequest(it, FileName("$it.pdf"), "blah, blah, blah"), bearerToken)
      }
    }
  }

  @Test
  fun `generate document throws exception for non-uploaded document categories that arent mapped`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())

    DocumentCategory.values().filter { !it.uploaded && !setOf(RECALL_NOTIFICATION, REVOCATION_ORDER, REASONS_FOR_RECALL, DOSSIER, LETTER_TO_PRISON).contains(it) }.forEach {
      assertThrows<WrongDocumentTypeException> {
        underTest.generateDocument(recallId, GenerateDocumentRequest(it, FileName("$it.pdf"), "blah, blah, blah"), bearerToken)
      }
    }
  }

  @Test
  fun `generate new recall notification`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val documentId = ::DocumentId.random()
    val fileName = FileName("RECALL_NOTIFICATION.pdf")

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { recallNotificationService.generateAndStorePdf(recallId, userId, fileName, details) } returns Mono.just(documentId)

    val result = underTest.generateDocument(recallId, GenerateDocumentRequest(RECALL_NOTIFICATION, fileName, details), bearerToken)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body, equalTo(NewDocumentResponse(documentId)))
      }
      .verifyComplete()

    verify { revocationOrderService wasNot Called }
    verify { reasonsForRecallService wasNot Called }
    verify { dossierService wasNot Called }
  }

  @Test
  fun `generate new revocation order`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val documentId = ::DocumentId.random()
    val fileName = FileName("REVOCATION_ORDER.pdf")

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { revocationOrderService.generateAndStorePdf(recallId, userId, fileName, details) } returns Mono.just(documentId)

    val result = underTest.generateDocument(recallId, GenerateDocumentRequest(REVOCATION_ORDER, fileName, details), bearerToken)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body, equalTo(NewDocumentResponse(documentId)))
      }
      .verifyComplete()

    verify { recallNotificationService wasNot Called }
    verify { reasonsForRecallService wasNot Called }
    verify { dossierService wasNot Called }
  }

  @Test
  fun `generate new reasons for recall`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val documentId = ::DocumentId.random()
    val fileName = FileName("REASONS_FOR_RECALL.pdf")

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { reasonsForRecallService.generateAndStorePdf(recallId, userId, fileName, details) } returns Mono.just(documentId)

    val result = underTest.generateDocument(recallId, GenerateDocumentRequest(REASONS_FOR_RECALL, fileName, details), bearerToken)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body, equalTo(NewDocumentResponse(documentId)))
      }
      .verifyComplete()

    verify { recallNotificationService wasNot Called }
    verify { revocationOrderService wasNot Called }
    verify { dossierService wasNot Called }
  }

  @Test
  fun `generate new dossier`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val documentId = ::DocumentId.random()
    val fileName = FileName("DOSSIER.pdf")

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { dossierService.generateAndStorePdf(recallId, userId, fileName, details) } returns Mono.just(documentId)

    val result = underTest.generateDocument(recallId, GenerateDocumentRequest(DOSSIER, fileName, details), bearerToken)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body, equalTo(NewDocumentResponse(documentId)))
      }
      .verifyComplete()

    verify { recallNotificationService wasNot Called }
    verify { revocationOrderService wasNot Called }
    verify { reasonsForRecallService wasNot Called }
  }

  @Test
  fun `generate new document without details`() {
    val bearerToken = "BEARER TOKEN"
    val userId = ::UserId.random()
    val documentId = ::DocumentId.random()
    val userDetails = mockk<UserDetails>()
    val fullName = FullName("Bertie Caseworker")
    val fileName = FileName("RECALL_NOTIFICATION.pdf")

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns TokenExtractor.Token(userId.toString())
    every { recallNotificationService.generateAndStorePdf(recallId, userId, fileName, null) } returns Mono.just(documentId)
    every { userDetailsService.get(userId) } returns userDetails
    every { userDetails.fullName() } returns fullName

    val result = underTest.generateDocument(recallId, GenerateDocumentRequest(RECALL_NOTIFICATION, fileName, null), bearerToken)

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body, equalTo(NewDocumentResponse(documentId)))
      }
      .verifyComplete()

    verify { revocationOrderService wasNot Called }
    verify { reasonsForRecallService wasNot Called }
    verify { dossierService wasNot Called }
  }
}
