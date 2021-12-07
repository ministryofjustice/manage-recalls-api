package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PhoneNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.random.randomNoms
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class RecallControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val recallService = mockk<RecallService>()
  private val prisonValidationService = mockk<PrisonValidationService>()
  private val courtValidationService = mockk<CourtValidationService>()
  private val tokenExtractor = mockk<TokenExtractor>()

  private val underTest =
    RecallController(
      recallRepository,
      userDetailsService,
      recallService,
      prisonValidationService,
      courtValidationService,
      tokenExtractor
    )

  private val recallId = ::RecallId.random()
  private val nomsNumber = NomsNumber("A1234AA")
  private val createdByUserId = ::UserId.random()
  private val firstName = FirstName("Barrie")
  private val middleNames = MiddleNames("Barnie")
  private val lastName = LastName("Badger")
  private val recallRequest = BookRecallRequest(nomsNumber, firstName, null, lastName)
  private val fileName = "fileName"
  private val now = OffsetDateTime.now()
  private val bookedByUserId = ::UserId.random()
  private val assignee = ::UserId.random()
  private val dossierCreatedByUserId = ::UserId.random()
  private val details = "Document details"

  private val recall = Recall(recallId, nomsNumber, createdByUserId, now, firstName, null, lastName)

  private val updateRecallRequest =
    UpdateRecallRequest(lastReleasePrison = PrisonId("ABC"), currentPrison = PrisonId("DEF"))

  private val recallResponse =
    RecallResponse(recallId, nomsNumber, createdByUserId, now, now, firstName, null, lastName, NameFormatCategory.FIRST_LAST, Status.BEING_BOOKED_ON)

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

  private fun newRecall() =
    Recall(::RecallId.random(), randomNoms(), createdByUserId, now, firstName, middleNames, lastName, licenceNameCategory = NameFormatCategory.FIRST_MIDDLE_LAST)

  private fun recallResponse(recall: Recall, status: Status) =
    RecallResponse(recall.recallId(), recall.nomsNumber, createdByUserId, now, now, firstName, middleNames, lastName, NameFormatCategory.FIRST_MIDDLE_LAST, status)

  private val beingBookedOnRecall = newRecall()
  private val bookedOnRecall = newRecall().copy(bookedByUserId = bookedByUserId.value)
  private val inAssessmentRecall = newRecall().copy(bookedByUserId = bookedByUserId.value, assignee = assignee.value)
  private val stoppedRecall = newRecall().copy(bookedByUserId = bookedByUserId.value, agreeWithRecall = AgreeWithRecall.NO_STOP)
  private val recallNotificationIssuedRecall = newRecall().copy(recallNotificationEmailSentDateTime = now)
  private val dossierInProgressRecall = newRecall().copy(recallNotificationEmailSentDateTime = now, assignee = assignee.value)
  private val dossierIssuedRecall = newRecall().copy(dossierCreatedByUserId = dossierCreatedByUserId.value)

  @Test
  fun `gets all recalls for a band FOUR_PLUS returns all recalls`() {
    val bearerToken = "Bearer header.payload"
    every { recallRepository.findAll() } returns listOf(beingBookedOnRecall, bookedOnRecall, inAssessmentRecall, stoppedRecall, recallNotificationIssuedRecall, dossierInProgressRecall, dossierIssuedRecall)
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(::UserId.random().toString())
    val userDetails = mockk<UserDetails>()
    every { userDetailsService.get(any()) } returns userDetails
    every { userDetailsService.find(any()) } returns userDetails
    every { userDetails.caseworkerBand } returns CaseworkerBand.FOUR_PLUS
    every { userDetails.fullName() } returns FullName("Mickey Mouse")

    val results = underTest.findAll(bearerToken)

    assertThat(results.size, equalTo(7))

    assertThat(
      results,
      List<RecallResponse>::containsAll,
      listOf(
        recallResponse(beingBookedOnRecall, Status.BEING_BOOKED_ON),
        recallResponse(bookedOnRecall, Status.BOOKED_ON).copy(bookedByUserId = bookedByUserId),
        recallResponse(inAssessmentRecall, Status.IN_ASSESSMENT).copy(bookedByUserId = bookedByUserId, assignee = assignee, assigneeUserName = FullName("Mickey Mouse")),
        recallResponse(stoppedRecall, Status.STOPPED).copy(bookedByUserId = bookedByUserId, agreeWithRecall = AgreeWithRecall.NO_STOP),
        recallResponse(recallNotificationIssuedRecall, Status.RECALL_NOTIFICATION_ISSUED).copy(recallNotificationEmailSentDateTime = now),
        recallResponse(dossierInProgressRecall, Status.DOSSIER_IN_PROGRESS).copy(recallNotificationEmailSentDateTime = now, assignee = assignee, assigneeUserName = FullName("Mickey Mouse")),
        recallResponse(dossierIssuedRecall, Status.DOSSIER_ISSUED).copy(dossierCreatedByUserId = dossierCreatedByUserId)
      )
    )
  }

  @Test
  fun `gets all recalls for a band THREE returns only recalls in BEING_BOOKED_ON, STOPPED, RECALL_NOTIFICATION_ISSUED, DOSSIER_IN_PROGRESS, DOSSIER_ISSUED statuses`() {
    val bearerToken = "Bearer header.payload"
    every { recallRepository.findAll() } returns listOf(beingBookedOnRecall, bookedOnRecall, inAssessmentRecall, stoppedRecall, recallNotificationIssuedRecall, dossierInProgressRecall, dossierIssuedRecall)
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(::UserId.random().toString())
    val userDetails = mockk<UserDetails>()
    every { userDetailsService.get(any()) } returns userDetails
    every { userDetailsService.find(any()) } returns userDetails
    every { userDetails.caseworkerBand } returns CaseworkerBand.THREE
    every { userDetails.fullName() } returns FullName("Mickey Mouse")

    val results = underTest.findAll(bearerToken)

    assertThat(results.size, equalTo(5))

    assertThat(
      results,
      List<RecallResponse>::containsAll,
      listOf(
        recallResponse(beingBookedOnRecall, Status.BEING_BOOKED_ON),
        recallResponse(stoppedRecall, Status.STOPPED).copy(bookedByUserId = bookedByUserId, agreeWithRecall = AgreeWithRecall.NO_STOP),
        recallResponse(recallNotificationIssuedRecall, Status.RECALL_NOTIFICATION_ISSUED).copy(recallNotificationEmailSentDateTime = now),
        recallResponse(dossierInProgressRecall, Status.DOSSIER_IN_PROGRESS).copy(recallNotificationEmailSentDateTime = now, assignee = assignee, assigneeUserName = FullName("Mickey Mouse")),
        recallResponse(dossierIssuedRecall, Status.DOSSIER_ISSUED).copy(dossierCreatedByUserId = dossierCreatedByUserId)
      )

    )
  }

  @Test
  fun `gets a recall`() {
    val document = Document(
      UUID.randomUUID(),
      UUID.randomUUID(),
      PART_A_RECALL_REPORT,
      fileName,
      1,
      details,
      now,
      createdByUserId.value
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

  @Test
  fun `set assignee for recall`() {
    val assignee = ::UserId.random()
    val assignedRecall = recall.copy(assignee = assignee.value)

    every { recallService.assignRecall(recallId, assignee) } returns assignedRecall
    every { userDetailsService.find(assignee) } returns UserDetails(
      assignee, FirstName("Bertie"), lastName, "", Email("b@b.com"), PhoneNumber("0987654321"),
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )

    val result = underTest.assignRecall(recallId, assignee)

    assertThat(
      result,
      equalTo(
        recallResponse.copy(
          assignee = assignee,
          assigneeUserName = FullName("Bertie Badger")
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
  fun `set assessByUserName, bookedByUserName for recall response`() {
    val firstNameAssessedBy = FirstName("Mickey")
    val lastNameAssessedBy = LastName("Mouse")
    val fullNameAssessedBy = FullName("Mickey Mouse")
    val firstNameBookedBy = FirstName("Natasha")
    val lastNameBookedBy = LastName("Romanoff")
    val fullNameBookedBy = FullName("Natasha Romanoff")
    val assessById = ::UserId.random()
    val bookedByUserId = ::UserId.random()

    val recallWithIds = recall.copy(assessedByUserId = assessById.value, bookedByUserId = bookedByUserId.value)

    every { userDetailsService.find(assessById) } returns UserDetails(
      assignee, firstNameAssessedBy, lastNameAssessedBy, "", Email("b@b.com"), PhoneNumber("0987654321"), CaseworkerBand.FOUR_PLUS, OffsetDateTime.now()
    )
    every { userDetailsService.find(bookedByUserId) } returns UserDetails(
      assignee, firstNameBookedBy, lastNameBookedBy, "", Email("b@b.com"), PhoneNumber("0987654321"), CaseworkerBand.FOUR_PLUS, OffsetDateTime.now()
    )

    every { recallRepository.getByRecallId(recallId) } returns recallWithIds

    val result = underTest.getRecall(recallId)

    assertThat(
      result,
      equalTo(
        recallResponse.copy(
          status = Status.BOOKED_ON,
          assessedByUserId = assessById,
          assessByUserName = fullNameAssessedBy,
          bookedByUserId = bookedByUserId,
          bookedByUserName = fullNameBookedBy
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
      Document(
        ::DocumentId.random(),
        recallId,
        PART_A_RECALL_REPORT,
        "part_a.pdf",
        1,
        null,
        OffsetDateTime.now(),
        createdByUserId
      )
    val partADoc2 = Document(
      ::DocumentId.random(),
      recallId,
      PART_A_RECALL_REPORT,
      "part_a.pdf",
      2,
      null,
      now,
      createdByUserId
    )
    val otherDoc1 = Document(::DocumentId.random(), recallId, OTHER, "mydoc.pdf", null, null, now, createdByUserId)
    val otherDoc2 = Document(::DocumentId.random(), recallId, OTHER, "mydoc.pdf", null, null, now, createdByUserId)
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
}
