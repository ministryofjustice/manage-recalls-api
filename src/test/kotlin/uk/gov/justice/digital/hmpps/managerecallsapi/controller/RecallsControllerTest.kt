package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.db.VersionedDocument
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison.LetterToPrisonService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomString
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UpdateRecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class RecallsControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val recallNotificationService = mockk<RecallNotificationService>()
  private val documentService = mockk<DocumentService>()
  private val dossierService = mockk<DossierService>()
  private val letterToPrisonService = mockk<LetterToPrisonService>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val updateRecallService = mockk<UpdateRecallService>()
  private val prisonValidationService = mockk<PrisonValidationService>()
  private val courtValidationService = mockk<CourtValidationService>()

  private val underTest =
    RecallsController(
      recallRepository,
      recallNotificationService,
      documentService,
      dossierService,
      letterToPrisonService,
      userDetailsService,
      updateRecallService,
      prisonValidationService,
      courtValidationService
    )

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
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
    val document = VersionedDocument(
      id = UUID.randomUUID(),
      recallId = UUID.randomUUID(),
      category = RecallDocumentCategory.PART_A_RECALL_REPORT,
      fileName = fileName,
      createdDateTime = OffsetDateTime.now()
    )
    val recallEmailReceivedDateTime = OffsetDateTime.now()
    val lastReleaseDate = LocalDate.now()
    val recall = Recall(
      recallId = recallId,
      nomsNumber = nomsNumber,
      documents = setOf(document),
      lastReleasePrison = PrisonId("BEL"),
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
      lastReleasePrison = PrisonId("BEL"),
      lastReleaseDate = lastReleaseDate,
      recallEmailReceivedDateTime = recallEmailReceivedDateTime,
    )
    assertThat(result, equalTo(expected))
  }

  @Suppress("ReactiveStreamsUnusedPublisher")
  @Test
  fun `get recall notification returns Recall Notification PDF`() {
    val recall = recallRequest.toRecall()
    val expectedPdf = randomString().toByteArray()
    val expectedBase64Pdf = expectedPdf.encodeToBase64String()
    val userId = UserId(UUID.randomUUID())

    every { recallNotificationService.getDocument(recall.recallId(), userId) } returns Mono.just(expectedPdf)

    val result = underTest.getRecallNotification(recall.recallId(), userId)

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
    val expectedBase64Pdf = expectedPdf.encodeToBase64String()

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

    every { documentService.getDocument(recallId1, documentId) } returns Pair(aRecallDocument, bytes)

    val response = underTest.getRecallDocument(recallId1, documentId)

    assertThat(response.statusCode, equalTo(HttpStatus.OK))
    val expected = GetDocumentResponse(
      documentId,
      aRecallDocument.category,
      content = bytes.encodeToBase64String(),
      fileName
    )
    assertThat(response.body, equalTo(expected))
  }

  @Test
  fun `get letter to prison returns expected PDF`() {
    val recall = recallRequest.toRecall()
    val expectedPdf = randomString().toByteArray()
    val expectedBase64Pdf = expectedPdf.encodeToBase64String()

    every { letterToPrisonService.getPdf(recall.recallId()) } returns Mono.just(expectedPdf)

    val result = underTest.getLetterToPrison(recall.recallId())

    StepVerifier
      .create(result)
      .assertNext {
        assertThat(it.body?.content, equalTo(expectedBase64Pdf))
      }
      .verifyComplete()
  }

  @Test
  fun `set assignee for recall`() {
    val assignee = ::UserId.random()
    val assignedRecall = Recall(recallId, nomsNumber, assignee = assignee)

    every { recallRepository.assignRecall(recallId, assignee) } returns assignedRecall
    every { userDetailsService.find(assignee) } returns UserDetails(assignee, FirstName("Bertie"), LastName("Badger"), "", Email("b@b.com"), PhoneNumber("0987654321"))

    val result = underTest.assignRecall(recallId, assignee)

    assertThat(result, equalTo(RecallResponse(recallId, nomsNumber, assignee = assignee, assigneeUserName = "Bertie Badger")))
  }

  @Test
  fun `set assignee for recall without user details`() {
    val assignee = ::UserId.random()
    val assignedRecall = Recall(recallId, nomsNumber, assignee = assignee)

    every { recallRepository.assignRecall(recallId, assignee) } returns assignedRecall
    every { userDetailsService.find(assignee) } returns null

    val result = underTest.assignRecall(recallId, assignee)

    assertThat(result, equalTo(RecallResponse(recallId, nomsNumber, assignee = assignee, assigneeUserName = null)))
  }

  @Test
  fun `unassign recall`() {
    val assignee = ::UserId.random()
    val unassignedRecall = Recall(recallId, nomsNumber)

    every { recallRepository.unassignRecall(recallId, assignee) } returns unassignedRecall

    val result = underTest.unassignRecall(recallId, assignee)

    assertThat(result, equalTo(RecallResponse(recallId, nomsNumber)))
  }

  private val recall = Recall(recallId, nomsNumber)

  private val updateRecallRequest =
    UpdateRecallRequest(lastReleasePrison = PrisonId("ABC"), currentPrison = PrisonId("DEF"))

  @Test
  fun `can update recall and return a response with all fields populated`() {
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { updateRecallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(RecallResponse(recallId, nomsNumber))))
  }

  @Test
  fun `can't update recall when current prison is not valid`() {
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns false
    every { updateRecallService.updateRecall(recallId, updateRecallRequest) } returns recall
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }

  @Test
  fun `can't update recall when last release prison is not valid`() {
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns false
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { updateRecallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }

  @Test
  fun `can't update recall when sentencing court is not valid`() {
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns false
    every { updateRecallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }
}
