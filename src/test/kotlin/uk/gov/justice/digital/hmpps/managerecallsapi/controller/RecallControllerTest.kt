package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
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

class RecallControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val recallNotificationService = mockk<RecallNotificationService>()
  private val dossierService = mockk<DossierService>()
  private val letterToPrisonService = mockk<LetterToPrisonService>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val recallService = mockk<RecallService>()
  private val prisonValidationService = mockk<PrisonValidationService>()
  private val courtValidationService = mockk<CourtValidationService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val underTest =
    RecallController(
      recallRepository,
      recallNotificationService,
      dossierService,
      letterToPrisonService,
      userDetailsService,
      recallService,
      prisonValidationService,
      courtValidationService,
      tokenExtractor
    )

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val createdByUserId = ::UserId.random()
  private val recallRequest = BookRecallRequest(nomsNumber, FirstName("Barrie"), null, LastName("Badger"))
  private val fileName = "fileName"
  private val now = OffsetDateTime.now()
  private val details = "Document details"

  private val recall = Recall(recallId, nomsNumber, createdByUserId, now, FirstName("Barrie"), null, LastName("Badger"))

  private val updateRecallRequest =
    UpdateRecallRequest(lastReleasePrison = PrisonId("ABC"), currentPrison = PrisonId("DEF"))

  private val recallResponse =
    RecallResponse(
      recallId,
      nomsNumber,
      createdByUserId,
      now,
      now,
      FirstName("Barrie"),
      null,
      LastName("Badger"),
      NameFormatCategory.FIRST_LAST,
      Status.BEING_BOOKED_ON
    )

  @Test
  fun `book recall returns request with id`() {
    val bearerToken = "Bearer header.payload"
    val userUuid = ::UserId.random()
    val recall = recallRequest.toRecall(userUuid).copy(createdDateTime = now, lastUpdatedDateTime = now)

    every { recallRepository.save(any()) } returns recall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userUuid.toString())

    val results = underTest.bookRecall(recallRequest, bearerToken)

    assertThat(results.body, equalTo(recallResponse.copy(recallId = recall.recallId(), createdByUserId = userUuid)))
  }

  @Test
  fun `gets all recalls`() {
    every { recallRepository.findAll() } returns listOf(recall)

    val results = underTest.findAll()

    assertThat(results, equalTo(listOf(recallResponse)))
  }

  @Test
  fun `gets a recall`() {
    val document = Document(
      id = UUID.randomUUID(),
      recallId = UUID.randomUUID(),
      category = PART_A_RECALL_REPORT,
      fileName = fileName,
      1,
      createdDateTime = now,
      details = details
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

    val expected = recallResponse.copy(
      documents = listOf(
        Api.RecallDocument(
          document.id(),
          document.category,
          fileName,
          document.version,
          document.createdDateTime,
          details
        )
      ),
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
    val recall = recallRequest.toRecall(::UserId.random())
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
    val recall = recallRequest.toRecall(::UserId.random())
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
    val recall = recallRequest.toRecall(::UserId.random())
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
        recallResponse.copy(
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
        recallResponse.copy(
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

    assertThat(result, equalTo(recallResponse))
  }

  @Test
  fun `can update recall and return a response with all fields populated`() {
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { recallService.updateRecall(recallId, updateRecallRequest) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest)

    assertThat(response, equalTo(ResponseEntity.ok(recallResponse)))
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
    val partADoc1 =
      Document(::DocumentId.random(), recallId, PART_A_RECALL_REPORT, "part_a.pdf", 1, OffsetDateTime.now(), null)
    val partADoc2 = Document(::DocumentId.random(), recallId, PART_A_RECALL_REPORT, "part_a.pdf", 2, now, null)
    val otherDoc1 = Document(::DocumentId.random(), recallId, OTHER, "mydoc.pdf", null, now, null)
    val otherDoc2 = Document(::DocumentId.random(), recallId, OTHER, "mydoc.pdf", null, now, null)
    val recallWithDocuments = recall.copy(documents = setOf(partADoc1, partADoc2, otherDoc1, otherDoc2))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDocuments

    val response = underTest.getRecall(recallId)

    assertThat(
      response,
      equalTo(
        recallResponse.copy(
          documents = listOf(
            Api.RecallDocument(partADoc2.id(), partADoc2.category, partADoc2.fileName, 2, now, null),
            Api.RecallDocument(otherDoc1.id(), otherDoc1.category, otherDoc1.fileName, null, now, null),
            Api.RecallDocument(otherDoc2.id(), otherDoc2.category, otherDoc2.fileName, null, now, null),
          )
        )
      )
    )
  }

  @Test
  fun `latestDocuments contains the latest details`() {
    val details2 = " First details"
    val details3 = " Second details"
    val partADoc1 =
      Document(::DocumentId.random(), recallId, PART_A_RECALL_REPORT, "part_a.pdf", 1, OffsetDateTime.now(), null)
    val partADoc2 = Document(::DocumentId.random(), recallId, PART_A_RECALL_REPORT, "part_a.pdf", 2, now, details2)
    val partADoc3 = Document(::DocumentId.random(), recallId, PART_A_RECALL_REPORT, "part_a.pdf", 2, now, details3)
    val recallWithDocuments = recall.copy(documents = setOf(partADoc1, partADoc2, partADoc3))

    every { recallRepository.getByRecallId(recallId) } returns recallWithDocuments

    val response = underTest.getRecall(recallId)

    assertThat(
      response,
      equalTo(
        recallResponse.copy(
          documents = listOf(
            Api.RecallDocument(partADoc2.id(), partADoc2.category, partADoc2.fileName, 2, now, details3),
            Api.RecallDocument(partADoc3.id(), partADoc2.category, partADoc2.fileName, 2, now, details3),
          )
        )
      )
    )
  }
}
