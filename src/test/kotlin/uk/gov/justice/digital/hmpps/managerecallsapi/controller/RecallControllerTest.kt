package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.managerecallsapi.config.InvalidPrisonOrCourtException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor.Token
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.LICENCE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OASYS_RISK_ASSESSMENT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.OTHER
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_A_RECALL_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PART_B_RISK_REPORT
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory.PREVIOUS_CONVICTIONS_SHEET
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.StopRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.Email
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FileName
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
import java.time.ZoneId
import java.util.UUID

class RecallControllerTest {
  private val recallRepository = mockk<RecallRepository>()
  private val userDetailsService = mockk<UserDetailsService>()
  private val recallService = mockk<RecallService>()
  private val prisonValidationService = mockk<PrisonValidationService>()
  private val courtValidationService = mockk<CourtValidationService>()
  private val tokenExtractor = mockk<TokenExtractor>()
  private val now = OffsetDateTime.now(ZoneId.of("UTC"))

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
  private val croNumber = CroNumber("ABC/1234A")
  private val recallRequest = BookRecallRequest(
    nomsNumber,
    firstName,
    null,
    lastName,
    croNumber,
    LocalDate.of(1999, 12, 1)
  )
  private val fileName = FileName("fileName")
  private val assessedByUserId = ::UserId.random()
  private val bookedByUserId = ::UserId.random()
  private val assignee = ::UserId.random()
  private val dossierCreatedByUserId = ::UserId.random()
  private val details = "Document details"

  private val recall = Recall(
    recallId, nomsNumber, createdByUserId, now, firstName, null, lastName,
    croNumber, LocalDate.of(1999, 12, 1)
  )

  private val updateRecallRequest =
    UpdateRecallRequest(lastReleasePrison = PrisonId("ABC"), currentPrison = PrisonId("DEF"))

  private val recallResponse =
    RecallResponse(
      recallId, nomsNumber, createdByUserId, now, now, firstName, null, lastName,
      croNumber, LocalDate.of(1999, 12, 1), Status.BEING_BOOKED_ON
    )

  @Test
  fun `book recall returns request with id`() {
    val bearerToken = "Bearer header.payload"
    val userUuid = ::UserId.random()

    every { recallService.bookRecall(any(), userUuid) } returns recall
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userUuid.toString())

    val results = underTest.bookRecall(recallRequest, bearerToken)

    assertThat(results, equalTo(recallResponse))
  }

  private fun newRecall() =
    Recall(
      ::RecallId.random(),
      randomNoms(),
      createdByUserId,
      now,
      firstName,
      middleNames,
      lastName,
      croNumber,
      LocalDate.of(1999, 12, 1),
      licenceNameCategory = NameFormatCategory.FIRST_MIDDLE_LAST
    )

  private fun recallLiteResponse(recall: Recall, status: Status) =
    RecallResponseLite(
      recall.recallId(),
      recall.nomsNumber,
      createdByUserId,
      now,
      now,
      firstName,
      middleNames,
      lastName,
      status,
      licenceNameCategory = NameFormatCategory.FIRST_MIDDLE_LAST
    )

  private val beingBookedOnRecall = newRecall()
  private val bookedOnRecall = newRecall().copy(bookedByUserId = bookedByUserId.value)
  private val inAssessmentRecall = newRecall().copy(bookedByUserId = bookedByUserId.value, assignee = assignee.value)
  private val stoppedRecall =
    newRecall().copy(
      bookedByUserId = bookedByUserId.value,
      stopRecord = StopRecord(StopReason.LEGAL_REASON, createdByUserId, OffsetDateTime.now())
    )
  private val inCustodyAwaitingDossierCreationRecall =
    newRecall().copy(inCustodyAtBooking = true, assessedByUserId = assessedByUserId.value)
  private val notInCustodyAssessedRecall = newRecall().copy(
    inCustodyAtBooking = false,
    inCustodyAtAssessment = false,
    assessedByUserId = assessedByUserId.value,
    assignee = assignee.value
  )
  private val inCustodyDossierInProgressRecall =
    newRecall().copy(inCustodyAtBooking = true, assessedByUserId = assessedByUserId.value, assignee = assignee.value)
  private val dossierIssuedRecall =
    newRecall().copy(dossierCreatedByUserId = dossierCreatedByUserId.value, confirmedRecallType = RecallType.FIXED)
  private val awaitingPartBRecall =
    newRecall().copy(dossierCreatedByUserId = dossierCreatedByUserId.value, confirmedRecallType = RecallType.STANDARD)

  @Test
  fun `gets all recalls for a band FOUR_PLUS returns all recalls`() {
    val bearerToken = "Bearer header.payload"
    every { recallRepository.findAll() } returns listOf(
      beingBookedOnRecall,
      bookedOnRecall,
      inAssessmentRecall,
      stoppedRecall,
      inCustodyAwaitingDossierCreationRecall,
      notInCustodyAssessedRecall,
      inCustodyDossierInProgressRecall,
      dossierIssuedRecall,
      awaitingPartBRecall
    )
    val userId = ::UserId.random()
    val currentUserDetails = mockk<UserDetails>()
    val assigneeUserDetails = mockk<UserDetails>()
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userId.toString())
    every { userDetailsService.getAll() } returns mapOf(userId to currentUserDetails, assignee to assigneeUserDetails)
    every { currentUserDetails.caseworkerBand } returns CaseworkerBand.FOUR_PLUS
    every { assigneeUserDetails.fullName() } returns FullName("Mickey Mouse")
    every { recallService.updateCustodyStatus(userId) } just Runs

    val results = underTest.findAll(bearerToken)

    assertThat(results.size, equalTo(9))

    assertThat(
      results,
      List<RecallResponseLite>::containsAll,
      listOf(
        recallLiteResponse(beingBookedOnRecall, Status.BEING_BOOKED_ON),
        recallLiteResponse(bookedOnRecall, Status.BOOKED_ON),
        recallLiteResponse(inAssessmentRecall, Status.IN_ASSESSMENT).copy(assigneeUserName = FullName("Mickey Mouse")),
        recallLiteResponse(stoppedRecall, Status.STOPPED),
        recallLiteResponse(inCustodyAwaitingDossierCreationRecall, Status.AWAITING_DOSSIER_CREATION),
        recallLiteResponse(notInCustodyAssessedRecall, Status.ASSESSED_NOT_IN_CUSTODY).copy(
          assigneeUserName = FullName("Mickey Mouse")
        ),
        recallLiteResponse(inCustodyDossierInProgressRecall, Status.DOSSIER_IN_PROGRESS).copy(
          assigneeUserName = FullName("Mickey Mouse")
        ),
        recallLiteResponse(dossierIssuedRecall, Status.DOSSIER_ISSUED),
        recallLiteResponse(awaitingPartBRecall, Status.AWAITING_PART_B),
      )
    )
  }

  @Test
  fun `gets all recalls for a band THREE returns only recalls in BEING_BOOKED_ON, STOPPED, RECALL_NOTIFICATION_ISSUED, DOSSIER_IN_PROGRESS, DOSSIER_ISSUED statuses`() {
    val bearerToken = "Bearer header.payload"
    every { recallRepository.findAll() } returns listOf(
      beingBookedOnRecall,
      bookedOnRecall,
      inAssessmentRecall,
      stoppedRecall,
      inCustodyAwaitingDossierCreationRecall,
      notInCustodyAssessedRecall,
      inCustodyDossierInProgressRecall,
      dossierIssuedRecall,
      awaitingPartBRecall
    )
    val userId = ::UserId.random()
    val currentUserDetails = mockk<UserDetails>()
    val assigneeUserDetails = mockk<UserDetails>()
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(userId.toString())
    every { userDetailsService.getAll() } returns mapOf(userId to currentUserDetails, assignee to assigneeUserDetails)
    every { currentUserDetails.caseworkerBand } returns CaseworkerBand.THREE
    every { assigneeUserDetails.fullName() } returns FullName("Mickey Mouse")
    every { recallService.updateCustodyStatus(userId) } just Runs

    val results = underTest.findAll(bearerToken)

    assertThat(results.size, equalTo(7))

    assertThat(
      results,
      List<RecallResponseLite>::containsAll,
      listOf(
        recallLiteResponse(beingBookedOnRecall, Status.BEING_BOOKED_ON),
        recallLiteResponse(stoppedRecall, Status.STOPPED),
        recallLiteResponse(inCustodyAwaitingDossierCreationRecall, Status.AWAITING_DOSSIER_CREATION),
        recallLiteResponse(notInCustodyAssessedRecall, Status.ASSESSED_NOT_IN_CUSTODY).copy(
          assigneeUserName = FullName("Mickey Mouse")
        ),
        recallLiteResponse(inCustodyDossierInProgressRecall, Status.DOSSIER_IN_PROGRESS).copy(
          assigneeUserName = FullName("Mickey Mouse")
        ),
        recallLiteResponse(dossierIssuedRecall, Status.DOSSIER_ISSUED),
        recallLiteResponse(awaitingPartBRecall, Status.AWAITING_PART_B),
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
    val userDetails = mockk<UserDetails>()
    every { userDetails.fullName() } returns FullName("Boris Badger")
    every { userDetailsService.get(createdByUserId) } returns userDetails
    every { recallRepository.getByRecallId(recallId) } returns recall

    val result = underTest.getRecall(recallId)

    val expected = recallResponse.copy(
      documents = listOf(
        Api.RecallDocument(
          document.id(),
          document.category,
          fileName,
          document.version,
          details,
          document.createdDateTime,
          FullName("Boris Badger")
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
    val assignedRecall = recall.copy(assignee = assignee.value, lastUpdatedByUserId = assignee.value)
    val bearerToken = "BEARER TOKEN"

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(assignee.toString())
    every { recallService.assignRecall(recallId, assignee, assignee) } returns assignedRecall
    every { userDetailsService.get(assignee) } returns UserDetails(
      assignee, FirstName("Bertie"), lastName, "", Email("b@b.com"), PhoneNumber("0987654321"),
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )

    val result = underTest.assignRecall(recallId, assignee, bearerToken)

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
  fun `set assessByUserName, bookedByUserName for recall response`() {
    val firstNameAssessedBy = FirstName("Mickey")
    val lastNameAssessedBy = LastName("Mouse")
    val fullNameAssessedBy = FullName("Mickey Mouse")
    val firstNameBookedBy = FirstName("Natasha")
    val lastNameBookedBy = LastName("Romanoff")
    val fullNameBookedBy = FullName("Natasha Romanoff")
    val firstNameDossierCreatedBy = FirstName("Natasha")
    val lastNameDossierCreatedBy = LastName("Romanoff")
    val fullNameDossierCreatedBy = FullName("Natasha Romanoff")
    val assessedByUserId = ::UserId.random()
    val bookedByUserId = ::UserId.random()
    val dossierCreatedByUserId = ::UserId.random()

    val recallWithIds = recall.copy(
      assessedByUserId = assessedByUserId.value,
      bookedByUserId = bookedByUserId.value,
      dossierCreatedByUserId = dossierCreatedByUserId.value,
      recommendedRecallType = RecallType.STANDARD,
      confirmedRecallType = RecallType.FIXED
    )

    every { userDetailsService.get(assessedByUserId) } returns UserDetails(
      assignee,
      firstNameAssessedBy,
      lastNameAssessedBy,
      "",
      Email("b@b.com"),
      PhoneNumber("0987654321"),
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )
    every { userDetailsService.get(bookedByUserId) } returns UserDetails(
      assignee,
      firstNameBookedBy,
      lastNameBookedBy,
      "",
      Email("b@b.com"),
      PhoneNumber("0987654321"),
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )
    every { userDetailsService.get(dossierCreatedByUserId) } returns UserDetails(
      assignee,
      firstNameDossierCreatedBy,
      lastNameDossierCreatedBy,
      "",
      Email("b@b.com"),
      PhoneNumber("0987654321"),
      CaseworkerBand.FOUR_PLUS,
      OffsetDateTime.now()
    )

    every { recallRepository.getByRecallId(recallId) } returns recallWithIds

    val result = underTest.getRecall(recallId)

    assertThat(
      result,
      equalTo(
        recallResponse.copy(
          status = Status.DOSSIER_ISSUED,
          assessedByUserId = assessedByUserId,
          assessedByUserName = fullNameAssessedBy,
          bookedByUserId = bookedByUserId,
          bookedByUserName = fullNameBookedBy,
          dossierCreatedByUserId = dossierCreatedByUserId,
          dossierCreatedByUserName = fullNameDossierCreatedBy,
          recommendedRecallType = RecallType.STANDARD,
          confirmedRecallType = RecallType.FIXED
        )
      )
    )
  }

  @Test
  fun `unassign recall`() {
    val assignee = ::UserId.random()
    val unassignedRecall = recall
    val bearerToken = "BEARER"

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(assignee.toString())
    every { recallService.unassignRecall(recallId, assignee) } returns unassignedRecall

    val result = underTest.unassignRecall(recallId, assignee, bearerToken)

    assertThat(result, equalTo(recallResponse))
  }

  @Test
  fun `can update recall and return a response with all fields populated`() {
    val bearerToken = "BEARER"
    val currentUserId = ::UserId.random()
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(currentUserId.toString())
    every { recallService.updateRecall(recallId, updateRecallRequest, currentUserId) } returns recall

    val response = underTest.updateRecall(recallId, updateRecallRequest, bearerToken)

    assertThat(response, equalTo(recallResponse))
  }

  @Test
  fun `can't update recall when current prison is not valid`() {
    val bearerToken = "BEARER"
    val currentUserId = ::UserId.random()
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns false
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(currentUserId.toString())
    every { recallService.updateRecall(recallId, updateRecallRequest, currentUserId) } returns recall

    val thrown = assertThrows<InvalidPrisonOrCourtException> {
      underTest.updateRecall(recallId, updateRecallRequest, bearerToken)
    }
    assertThat(
      thrown.message,
      equalTo("validAndActiveCurrentPrison=[false], validLastReleasePrison=[true], validSentencingCourt=[true]")
    )
  }

  @Test
  fun `can't update recall when last release prison is not valid`() {
    val bearerToken = "BEARER"
    val currentUserId = ::UserId.random()
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns false
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns true
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(currentUserId.toString())
    every { recallService.updateRecall(recallId, updateRecallRequest, currentUserId) } returns recall

    val thrown = assertThrows<InvalidPrisonOrCourtException> {
      underTest.updateRecall(recallId, updateRecallRequest, bearerToken)
    }
    assertThat(
      thrown.message,
      equalTo("validAndActiveCurrentPrison=[true], validLastReleasePrison=[false], validSentencingCourt=[true]")
    )
  }

  @Test
  fun `can't update recall when sentencing court is not valid`() {
    val bearerToken = "BEARER"
    val currentUserId = ::UserId.random()
    every { prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) } returns true
    every { prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) } returns true
    every { courtValidationService.isValid(updateRecallRequest.sentencingCourt) } returns false
    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(currentUserId.toString())
    every { recallService.updateRecall(recallId, updateRecallRequest, currentUserId) } returns recall

    val thrown = assertThrows<InvalidPrisonOrCourtException> {
      underTest.updateRecall(recallId, updateRecallRequest, bearerToken)
    }
    assertThat(
      thrown.message,
      equalTo("validAndActiveCurrentPrison=[true], validLastReleasePrison=[true], validSentencingCourt=[false]")
    )
  }

  @Test
  fun `latestDocuments contains the latest of each versioned category and all unversioned docs`() {
    val partADoc1 =
      Document(
        ::DocumentId.random(),
        recallId,
        PART_A_RECALL_REPORT,
        FileName("part_a.pdf"),
        1,
        null,
        OffsetDateTime.now(),
        createdByUserId
      )
    val partADoc2 = Document(
      ::DocumentId.random(),
      recallId,
      PART_A_RECALL_REPORT,
      FileName("part_a.pdf"),
      2,
      details,
      now,
      createdByUserId
    )
    val otherDoc1 =
      Document(::DocumentId.random(), recallId, OTHER, FileName("mydoc.pdf"), null, null, now, createdByUserId)
    val otherDoc2 =
      Document(::DocumentId.random(), recallId, OTHER, FileName("mydoc.pdf"), null, null, now, createdByUserId)
    val recallWithDocuments = recall.copy(documents = setOf(partADoc1, partADoc2, otherDoc1, otherDoc2))
    val userDetails = mockk<UserDetails>()
    val fullName = FullName("Bertie Badger")

    every { recallRepository.getByRecallId(recallId) } returns recallWithDocuments
    every { userDetailsService.get(createdByUserId) } returns userDetails
    every { userDetails.fullName() } returns fullName

    val response = underTest.getRecall(recallId)

    assertThat(
      response,
      equalTo(
        recallResponse.copy(
          documents = listOf(
            Api.RecallDocument(partADoc2.id(), partADoc2.category, partADoc2.fileName, 2, details, now, fullName),
            Api.RecallDocument(otherDoc1.id(), otherDoc1.category, otherDoc1.fileName, null, null, now, fullName),
            Api.RecallDocument(otherDoc2.id(), otherDoc2.category, otherDoc2.fileName, null, null, now, fullName),
          )
        )
      )
    )
  }

  @Test
  fun `can set recommendedRecallType`() {
    val bearerToken = "BEARER"
    val currentUserId = ::UserId.random()
    val recallType = RecallType.values().random()

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(currentUserId.toString())
    every { recallService.updateRecommendedRecallType(recallId, recallType, currentUserId) } returns recall.copy(
      recommendedRecallType = recallType
    )

    val response =
      underTest.updateRecommendedRecallType(recallId, RecommendedRecallTypeRequest(recallType), bearerToken)
    assertThat(response.recommendedRecallType, equalTo(recallType))

    verify { recallService.updateRecommendedRecallType(recallId, recallType, currentUserId) }
  }

  @Test
  fun `can set confirmedRecallType and details`() {
    val bearerToken = "BEARER"
    val currentUserId = ::UserId.random()
    val recallType = RecallType.values().random()
    val confirmedRecallTypeDetail = "Some details"
    val request = ConfirmedRecallTypeRequest(recallType, confirmedRecallTypeDetail)

    every { tokenExtractor.getTokenFromHeader(bearerToken) } returns Token(currentUserId.toString())
    every { recallService.confirmRecallType(recallId, request, currentUserId) } returns recall.copy(
      confirmedRecallType = recallType, confirmedRecallTypeDetail = confirmedRecallTypeDetail
    )

    val response = underTest.confirmedRecallType(recallId, request, bearerToken)
    assertThat(response.confirmedRecallType, equalTo(recallType))
    assertThat(response.confirmedRecallTypeDetail, equalTo(confirmedRecallTypeDetail))

    verify { recallService.confirmRecallType(recallId, request, currentUserId) }
  }

  @Test
  fun `missingDocuments is null when probationInfo not set`() {
    every { recallRepository.getByRecallId(recallId) } returns recall

    val response = underTest.getRecall(recallId)

    assertThat(response.missingDocuments, equalTo(null))
  }

  @Test
  fun `missingDocuments contains PART_A & LICENCE as required and OASYS and PRECONS as desired when probationInfo is set but no documents uploaded`() {
    every { recallRepository.getByRecallId(recallId) } returns recall.copy(
      probationInfo = ProbationInfo(
        FullName("Mr Probation"),
        PhoneNumber("01234 567890"),
        Email("mr@probation.com"),
        LocalDeliveryUnit.CHANNEL_ISLANDS,
        FullName("Chief"),
      )
    )

    val response = underTest.getRecall(recallId)

    assertThat(response.missingDocuments!!.required, equalTo(setOf(PART_A_RECALL_REPORT, LICENCE)))
    assertThat(
      response.missingDocuments!!.desired,
      equalTo(
        setOf(OASYS_RISK_ASSESSMENT, PREVIOUS_CONVICTIONS_SHEET)
      )
    )
  }

  @Test
  fun `missingDocuments contains LICENCE as required and OASYS and PRECONS as desired when probationInfo is set and only PART_A document uploaded`() {
    every { recallRepository.getByRecallId(recallId) } returns recall.copy(
      probationInfo = ProbationInfo(
        FullName("Mr Probation"),
        PhoneNumber("01234 567890"),
        Email("mr@probation.com"),
        LocalDeliveryUnit.CHANNEL_ISLANDS,
        FullName("Chief"),
      ),
      documents = setOf(document(PART_A_RECALL_REPORT))
    )

    val userDetails = mockk<UserDetails>()
    every { userDetails.fullName() } returns FullName("Boris Badger")
    every { userDetailsService.get(createdByUserId) } returns userDetails

    val response = underTest.getRecall(recallId)

    assertThat(response.missingDocuments!!.required, equalTo(setOf(LICENCE)))
    assertThat(
      response.missingDocuments!!.desired,
      equalTo(
        setOf(OASYS_RISK_ASSESSMENT, PREVIOUS_CONVICTIONS_SHEET)
      )
    )
  }

  @Test
  fun `missingDocuments null when probationInfo is set and PART_A, LICENCE, PRECONS & OASYS documents uploaded`() {
    every { recallRepository.getByRecallId(recallId) } returns recall.copy(
      probationInfo = ProbationInfo(
        FullName("Mr Probation"),
        PhoneNumber("01234 567890"),
        Email("mr@probation.com"),
        LocalDeliveryUnit.CHANNEL_ISLANDS,
        FullName("Chief"),
      ),
      documents = setOf(
        document(PART_A_RECALL_REPORT),
        document(LICENCE),
        document(OASYS_RISK_ASSESSMENT),
        document(PREVIOUS_CONVICTIONS_SHEET),
      )
    )

    val userDetails = mockk<UserDetails>()
    every { userDetails.fullName() } returns FullName("Boris Badger")
    every { userDetailsService.get(createdByUserId) } returns userDetails

    val response = underTest.getRecall(recallId)

    assertThat(response.missingDocuments, equalTo(null))
  }

  @Test
  fun `missingDocuments null for Standard recall when Dossier NOT created and PART_B not uploaded`() {
    every { recallRepository.getByRecallId(recallId) } returns recall.copy(
      recommendedRecallType = RecallType.STANDARD,
      confirmedRecallType = RecallType.STANDARD,
      probationInfo = ProbationInfo(
        FullName("Mr Probation"),
        PhoneNumber("01234 567890"),
        Email("mr@probation.com"),
        LocalDeliveryUnit.CHANNEL_ISLANDS,
        FullName("Chief"),
      ),
      documents = setOf(
        document(PART_A_RECALL_REPORT),
        document(LICENCE),
        document(OASYS_RISK_ASSESSMENT),
        document(PREVIOUS_CONVICTIONS_SHEET),
      ),
    )

    val userDetails = mockk<UserDetails>()
    every { userDetails.fullName() } returns FullName("Boris Badger")
    every { userDetailsService.get(createdByUserId) } returns userDetails

    val response = underTest.getRecall(recallId)

    assertThat(response.missingDocuments, equalTo(null))
  }

  @Test
  fun `missingDocuments contains PART_B as required for Standard recall when Dossier created but PART_B not uploaded`() {
    every { recallRepository.getByRecallId(recallId) } returns recall.copy(
      probationInfo = ProbationInfo(
        FullName("Mr Probation"),
        PhoneNumber("01234 567890"),
        Email("mr@probation.com"),
        LocalDeliveryUnit.CHANNEL_ISLANDS,
        FullName("Chief"),
      ),
      recommendedRecallType = RecallType.STANDARD,
      confirmedRecallType = RecallType.STANDARD,
      dossierCreatedByUserId = createdByUserId.value,
      documents = setOf(
        document(PART_A_RECALL_REPORT),
        document(LICENCE),
        document(OASYS_RISK_ASSESSMENT),
        document(PREVIOUS_CONVICTIONS_SHEET),
      ),
    )

    val userDetails = mockk<UserDetails>()
    every { userDetails.fullName() } returns FullName("Boris Badger")
    every { userDetailsService.get(createdByUserId) } returns userDetails

    val response = underTest.getRecall(recallId)

    assertThat(response.missingDocuments!!.required, equalTo(setOf(PART_B_RISK_REPORT)))
    assertThat(response.missingDocuments!!.desired, equalTo(emptySet()))
  }

  @Test
  fun `missingDocuments null for Standard recall when Dossier created and PART_B uploaded`() {
    every { recallRepository.getByRecallId(recallId) } returns recall.copy(
      probationInfo = ProbationInfo(
        FullName("Mr Probation"),
        PhoneNumber("01234 567890"),
        Email("mr@probation.com"),
        LocalDeliveryUnit.CHANNEL_ISLANDS,
        FullName("Chief"),
      ),
      recommendedRecallType = RecallType.STANDARD,
      confirmedRecallType = RecallType.STANDARD,
      dossierCreatedByUserId = createdByUserId.value,
      documents = setOf(
        document(PART_A_RECALL_REPORT),
        document(LICENCE),
        document(OASYS_RISK_ASSESSMENT),
        document(PREVIOUS_CONVICTIONS_SHEET),
        document(PART_B_RISK_REPORT),
      ),
    )

    val userDetails = mockk<UserDetails>()
    every { userDetails.fullName() } returns FullName("Boris Badger")
    every { userDetailsService.get(createdByUserId) } returns userDetails

    val response = underTest.getRecall(recallId)

    assertThat(response.missingDocuments, equalTo(null))
  }

  private fun document(category: DocumentCategory) =
    Document(::DocumentId.random(), recallId, category, FileName("test.pdf"), 1, null, now, createdByUserId)
}
