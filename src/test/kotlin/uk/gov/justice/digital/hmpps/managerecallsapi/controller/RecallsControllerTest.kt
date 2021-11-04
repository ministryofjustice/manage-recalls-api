package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison.LetterToPrisonService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
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
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class RecallsControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val recallNotificationService = mockk<RecallNotificationService>()
  private val dossierService = mockk<DossierService>()
  private val letterToPrisonService = mockk<LetterToPrisonService>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val recallService = mockk<RecallService>()
  private val prisonValidationService = mockk<PrisonValidationService>()
  private val courtValidationService = mockk<CourtValidationService>()

  private val underTest =
    RecallsController(
      recallRepository,
      recallNotificationService,
      dossierService,
      letterToPrisonService,
      userDetailsService,
      recallService,
      prisonValidationService,
      courtValidationService
    )

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val createdByUserId = ::UserId.random()
  private val recallRequest = BookRecallRequest(nomsNumber, createdByUserId)
  private val fileName = "fileName"
  private val now = OffsetDateTime.now()

  private val recall = Recall(recallId, nomsNumber, createdByUserId, now, now)

  private val updateRecallRequest =
    UpdateRecallRequest(lastReleasePrison = PrisonId("ABC"), currentPrison = PrisonId("DEF"))

  @Test
  fun `book recall returns request with id`() {
    val recall = recallRequest.toRecall().copy(createdDateTime = now, lastUpdatedDateTime = now)

    every { recallRepository.save(any()) } returns recall

    val results = underTest.bookRecall(recallRequest)

    val expected = RecallResponse(recall.recallId(), nomsNumber, createdByUserId, now, now, Status.BEING_BOOKED_ON)
    assertThat(results.body, equalTo(expected))
  }

  @Test
  fun `gets all recalls`() {
    every { recallRepository.findAll() } returns listOf(recall)

    val results = underTest.findAll()

    assertThat(results, equalTo(listOf(RecallResponse(recallId, nomsNumber, createdByUserId, now, now, Status.BEING_BOOKED_ON))))
  }

  @Test
  fun `gets a recall`() {
    val document = Document(
      id = UUID.randomUUID(),
      recallId = UUID.randomUUID(),
      category = PART_A_RECALL_REPORT,
      fileName = fileName,
      1,
      createdDateTime = now
    )
    val recallEmailReceivedDateTime = now
    val lastReleaseDate = LocalDate.now()
    val recall = recall.copy(
      documents = setOf(document),
      lastReleasePrison = PrisonId("BEL"),
      lastReleaseDate = lastReleaseDate,
      recallEmailReceivedDateTime = recallEmailReceivedDateTime
    )
    every { recallRepository.getByRecallId(recallId) } returns recall

    val result = underTest.getRecall(recallId)

    val expected = RecallResponse(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      now,
      Status.BEING_BOOKED_ON,
      documents = listOf(ApiRecallDocument(document.id(), document.category, fileName, document.version, document.createdDateTime)),
      lastReleasePrison = PrisonId("BEL"),
      lastReleaseDate = lastReleaseDate,
      recallEmailReceivedDateTime = recallEmailReceivedDateTime,
      recallAssessmentDueDateTime = recallEmailReceivedDateTime.plusHours(24)
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
    val assignedRecall = recall.copy(assignee = assignee.value)

    every { recallService.assignRecall(recallId, assignee) } returns assignedRecall
    every { userDetailsService.find(assignee) } returns UserDetails(
      assignee, FirstName("Bertie"), LastName("Badger"), "", Email("b@b.com"), PhoneNumber("0987654321"),
      OffsetDateTime.now()
    )

    val result = underTest.assignRecall(recallId, assignee)

    assertThat(
      result,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          now,
          Status.BEING_BOOKED_ON,
          assignee = assignee,
          assigneeUserName = "Bertie Badger"
        )
      )
    )
  }

  @Test
  fun `set assignee for recall without user details`() {
    val assignee = ::UserId.random()
    val assignedRecall = recall.copy(assignee = assignee.value)

    every { recallService.assignRecall(recallId, assignee) } returns assignedRecall
    every { userDetailsService.find(assignee) } returns null

    val result = underTest.assignRecall(recallId, assignee)

    assertThat(
      result,
      equalTo(
        RecallResponse(
          recallId,
          nomsNumber,
          createdByUserId,
          now,
          now,
          Status.BEING_BOOKED_ON,
          assignee = assignee,
          assigneeUserName = null
        )
      )
    )
  }

  @Test
  fun `unassign recall`() {
    val assignee = ::UserId.random()
    val unassignedRecall = recall

    every { recallService.unassignRecall(recallId, assignee) } returns unassignedRecall

    val result = underTest.unassignRecall(recallId, assignee)

    assertThat(result, equalTo(RecallResponse(recallId, nomsNumber, createdByUserId, now, now, Status.BEING_BOOKED_ON)))
  }

  @Test
  fun `can update recall and return a response with all fields populated`() {
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { recallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(RecallResponse(recallId, nomsNumber, createdByUserId, now, now, Status.BEING_BOOKED_ON))))
  }

  @Test
  fun `can't update recall when current prison is not valid`() {
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns false
    every { recallService.updateRecall(recallId, updateRecallRequest) } returns recall
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }

  @Test
  fun `can't update recall when last release prison is not valid`() {
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns false
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { recallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }

  @Test
  fun `can't update recall when sentencing court is not valid`() {
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns false
    every { recallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.badRequest().build()))
  }

  @Test
  fun `latestDocuments contains the latest of each versioned category and all unversioned docs`() {
    val partADoc1 = Document(::DocumentId.random(), recallId, PART_A_RECALL_REPORT, "part_a.pdf", 1, OffsetDateTime.now())
    val partADoc2 = Document(::DocumentId.random(), recallId, PART_A_RECALL_REPORT, "part_a.pdf", 2, now)
    val otherDoc1 = Document(::DocumentId.random(), recallId, OTHER, "mydoc.pdf", null, now)
    val otherDoc2 = Document(::DocumentId.random(), recallId, OTHER, "mydoc.pdf", null, now)
    val recallWithDocuments = recall.copy(documents = setOf(partADoc1, partADoc2, otherDoc1, otherDoc2))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDocuments

    val response = underTest.getRecall(recallId)

    assertThat(
      response,
      equalTo(
        RecallResponse(
          recallId, nomsNumber, createdByUserId, now, now, Status.BEING_BOOKED_ON,
          documents = listOf(
            ApiRecallDocument(partADoc2.id(), partADoc2.category, partADoc2.fileName, 2, now),
            ApiRecallDocument(otherDoc1.id(), otherDoc1.category, otherDoc1.fileName, null, now),
            ApiRecallDocument(otherDoc2.id(), otherDoc2.category, otherDoc2.fileName, null, now),
          )
        )
      )
    )
  }
}
