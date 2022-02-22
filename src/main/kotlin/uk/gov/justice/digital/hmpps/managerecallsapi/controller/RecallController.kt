package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.config.InvalidPrisonOrCourtException
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand.FOUR_PLUS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand.THREE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Note
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RescindRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.UserDetails
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CroNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NoteId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RescindRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val userDetailsService: UserDetailsService,
  @Autowired private val recallService: RecallService,
  @Autowired private val prisonValidationService: PrisonValidationService,
  @Autowired private val courtValidationService: CourtValidationService,
  @Autowired private val tokenExtractor: TokenExtractor,
  @Autowired private val clock: Clock,
) {

  @PostMapping("/recalls")
  @ResponseStatus(HttpStatus.CREATED)
  fun bookRecall(
    @RequestBody bookRecallRequest: BookRecallRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): RecallResponse =
    tokenExtractor.getTokenFromHeader(bearerToken).userUuid().let { currentUserId ->
      recallRepository.save(bookRecallRequest.toRecall(currentUserId, clock), currentUserId).toResponse()
    }

  @GetMapping("/recalls")
  fun findAll(@RequestHeader("Authorization") bearerToken: String): List<RecallResponseLite> {
    // Fetching all users to prevent repeated requests to database
    val users = userDetailsService.getAll()
    val currentUserId = tokenExtractor.getTokenFromHeader(bearerToken).userUuid()
    val band = users[currentUserId]!!.caseworkerBand

    recallService.updateCustodyStatus(currentUserId)

    return recallRepository.findAll().filter { it.status().visibilityBands.contains(band) }
      .map { it.toResponseLite(users) }
  }

  @PostMapping("/recalls/search")
  fun recallSearch(@RequestBody searchRequest: RecallSearchRequest): List<RecallResponse> =
    recallRepository.search(searchRequest).map { it.toResponse() }

  @GetMapping("/recalls/{recallId}")
  fun getRecall(@PathVariable("recallId") recallId: RecallId): RecallResponse =
    recallRepository.getByRecallId(recallId).toResponse()

  @PatchMapping("/recalls/{recallId}")
  @ResponseStatus(HttpStatus.OK)
  fun updateRecall(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody updateRecallRequest: UpdateRecallRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): RecallResponse {
    val validAndActiveCurrentPrison = prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison)
    val validLastReleasePrison = prisonValidationService.isValid(updateRecallRequest.lastReleasePrison)
    val validSentencingCourt = courtValidationService.isValid(updateRecallRequest.sentencingCourt)
    return if (validAndActiveCurrentPrison &&
      validLastReleasePrison &&
      validSentencingCourt
    ) {
      val token = tokenExtractor.getTokenFromHeader(bearerToken)
      recallService.updateRecall(recallId, updateRecallRequest, token.userUuid()).toResponse()
    } else {
      throw InvalidPrisonOrCourtException(validAndActiveCurrentPrison, validLastReleasePrison, validSentencingCourt)
    }
  }

  @PostMapping("/recalls/{recallId}/assignee/{assignee}")
  fun assignRecall(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("assignee") assignee: UserId,
    @RequestHeader("Authorization") bearerToken: String
  ): RecallResponse {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    return recallService.assignRecall(recallId, assignee, token.userUuid()).toResponse()
  }

  @DeleteMapping("/recalls/{recallId}/assignee/{assignee}")
  fun unassignRecall(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("assignee") assignee: UserId,
    @RequestHeader("Authorization") bearerToken: String
  ): RecallResponse {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)

    return recallService.unassignRecall(recallId, assignee, token.userUuid()).toResponse()
  }

  fun Recall.toResponseLite(users: Map<UserId, UserDetails>) =
    RecallResponseLite(
      recallId = recallId(),
      nomsNumber = nomsNumber,
      createdByUserId = createdByUserId(),
      createdDateTime = createdDateTime,
      lastUpdatedDateTime = lastUpdatedDateTime,
      firstName = firstName,
      middleNames = middleNames,
      lastName = lastName,
      licenceNameCategory = licenceNameCategory,
      status = status(),
      inCustodyAtBooking = inCustodyAtBooking,
      inCustodyAtAssessment = inCustodyAtAssessment,
      dossierEmailSentDate = dossierEmailSentDate,
      dossierTargetDate = dossierTargetDate,
      recallAssessmentDueDateTime = recallAssessmentDueDateTime(),
      assigneeUserName = assignee()?.let { users[it]!!.fullName() },
    )

  fun Recall.toResponse() = RecallResponse(
    recallId = recallId(),
    nomsNumber = nomsNumber,
    createdByUserId = createdByUserId(),
    createdDateTime = createdDateTime,
    lastUpdatedDateTime = lastUpdatedDateTime,
    firstName = firstName,
    middleNames = middleNames,
    lastName = lastName,
    croNumber = croNumber,
    dateOfBirth = dateOfBirth,
    licenceNameCategory = licenceNameCategory,
    status = status(),
    documents = latestDocuments(documents),
    missingDocumentsRecords = missingDocumentsRecords.map { record -> record.toResponse(documents) },
    lastKnownAddresses = lastKnownAddresses.map { address -> address.toResponse() },
    rescindRecords = rescindRecords.map { record -> record.toResponse(documents) },
    notes = notes.map { note -> note.toResponse(documents) },
    recallLength = recallLength,
    lastReleasePrison = lastReleasePrison,
    lastReleaseDate = lastReleaseDate,
    recallEmailReceivedDateTime = recallEmailReceivedDateTime,
    localPoliceForceId = localPoliceForceId,
    inCustodyAtBooking = inCustodyAtBooking,
    inCustodyAtAssessment = inCustodyAtAssessment,
    contraband = contraband,
    contrabandDetail = contrabandDetail,
    vulnerabilityDiversity = vulnerabilityDiversity,
    vulnerabilityDiversityDetail = vulnerabilityDiversityDetail,
    mappaLevel = mappaLevel,
    sentenceDate = sentencingInfo?.sentenceDate,
    licenceExpiryDate = sentencingInfo?.licenceExpiryDate,
    sentenceExpiryDate = sentencingInfo?.sentenceExpiryDate,
    sentencingCourt = sentencingInfo?.sentencingCourt,
    indexOffence = sentencingInfo?.indexOffence,
    conditionalReleaseDate = sentencingInfo?.conditionalReleaseDate,
    sentenceLength = sentencingInfo?.sentenceLength?.let {
      Api.SentenceLength(
        it.sentenceYears,
        it.sentenceMonths,
        it.sentenceDays
      )
    },
    bookingNumber = bookingNumber,
    probationOfficerName = probationInfo?.probationOfficerName,
    probationOfficerPhoneNumber = probationInfo?.probationOfficerPhoneNumber,
    probationOfficerEmail = probationInfo?.probationOfficerEmail,
    localDeliveryUnit = probationInfo?.localDeliveryUnit,
    authorisingAssistantChiefOfficer = probationInfo?.authorisingAssistantChiefOfficer,
    licenceConditionsBreached = licenceConditionsBreached,
    reasonsForRecall = reasonsForRecall.toList(),
    reasonsForRecallOtherDetail = reasonsForRecallOtherDetail,
    agreeWithRecall = agreeWithRecall,
    agreeWithRecallDetail = agreeWithRecallDetail,
    currentPrison = currentPrison,
    additionalLicenceConditions = additionalLicenceConditions,
    additionalLicenceConditionsDetail = additionalLicenceConditionsDetail,
    differentNomsNumber = differentNomsNumber,
    differentNomsNumberDetail = differentNomsNumberDetail,
    recallNotificationEmailSentDateTime = recallNotificationEmailSentDateTime,
    dossierEmailSentDate = dossierEmailSentDate,
    previousConvictionMainNameCategory = previousConvictionMainNameCategory,
    hasDossierBeenChecked = hasDossierBeenChecked,
    previousConvictionMainName = previousConvictionMainName,
    assessedByUserId = assessedByUserId(),
    bookedByUserId = bookedByUserId(),
    dossierCreatedByUserId = dossierCreatedByUserId(),
    dossierTargetDate = dossierTargetDate,
    assignee = assignee(),
    assigneeUserName = assignee()?.let { userDetailsService.get(it).fullName() },
    recallAssessmentDueDateTime = recallAssessmentDueDateTime(),
    assessedByUserName = assessedByUserId()?.let { userDetailsService.get(it).fullName() },
    bookedByUserName = bookedByUserId()?.let { userDetailsService.get(it).fullName() },
    dossierCreatedByUserName = dossierCreatedByUserId()?.let { userDetailsService.get(it).fullName() },
    lastKnownAddressOption = lastKnownAddressOption,
    arrestIssues = arrestIssues,
    arrestIssuesDetail = arrestIssuesDetail,
    warrantReferenceNumber = warrantReferenceNumber,
    stopReason = stopRecord?.stopReason,
    stopDateTime = stopRecord?.stopDateTime,
    stopByUserName = stopRecord?.let { userDetailsService.get(it.stopByUserId()).fullName() },
    returnedToCustodyDateTime = returnedToCustody?.returnedToCustodyDateTime,
    returnedToCustodyNotificationDateTime = returnedToCustody?.notificationDateTime,
  )

  private fun latestDocuments(documents: Set<Document>): List<Api.RecallDocument> {
    val partitionedDocs = documents.partition { it.category.versioned }
    val versionedDocs = partitionedDocs.first
    val unversionedDocs = partitionedDocs.second
    val latestDocuments =
      versionedDocs.groupBy { it.category }.values.map { it.maxByOrNull { d -> d.version!! }!! } + unversionedDocs
    return latestDocuments.map {
      Api.RecallDocument(
        it.id(),
        it.category,
        it.fileName,
        it.version,
        it.details,
        it.createdDateTime,
        userDetailsService.get(it.createdByUserId()).fullName()
      )
    }
  }

  fun MissingDocumentsRecord.toResponse(documents: Set<Document>) = Api.MissingDocumentsRecord(
    id(),
    categories.toList(),
    emailId(),
    documents.first { it.id == emailId }.fileName,
    details,
    version,
    userDetailsService.get(createdByUserId()).fullName(),
    createdDateTime
  )

  fun RescindRecord.toResponse(documents: Set<Document>) = Api.RescindRecord(
    id(),
    version,
    userDetailsService.get(createdByUserId()).fullName(),
    createdDateTime,
    lastUpdatedDateTime,
    requestDetails,
    requestEmailId(),
    documents.first { it.id == requestEmailId }.fileName,
    requestEmailReceivedDate,
    approved,
    decisionDetails,
    decisionEmailId(),
    documents.firstOrNull { it.id == decisionEmailId }?.fileName,
    decisionEmailSentDate
  )

  fun Note.toResponse(documents: Set<Document>) = Api.Note(
    id(),
    subject,
    details,
    index,
    documentId(),
    documents.firstOrNull { it.id == documentId }?.fileName,
    userDetailsService.get(createdByUserId()).fullName(),
    createdDateTime,
  )

  fun LastKnownAddress.toResponse() = Api.LastKnownAddress(
    id(),
    line1,
    line2,
    town,
    postcode,
    source,
    index,
    userDetailsService.get(createdByUserId()).fullName(),
    createdDateTime
  )
}

fun BookRecallRequest.toRecall(userUuid: UserId, clock: Clock): Recall {
  val now = OffsetDateTime.now(clock)
  return Recall(
    ::RecallId.random(),
    nomsNumber,
    userUuid,
    now,
    firstName,
    middleNames,
    lastName,
    croNumber,
    dateOfBirth
  )
}

data class BookRecallRequest(
  val nomsNumber: NomsNumber,
  val firstName: FirstName,
  val middleNames: MiddleNames?,
  val lastName: LastName,
  val croNumber: CroNumber,
  val dateOfBirth: LocalDate,
)

data class RecallResponseLite(
  val recallId: RecallId,
  val nomsNumber: NomsNumber,
  val createdByUserId: UserId,
  val createdDateTime: OffsetDateTime,
  val lastUpdatedDateTime: OffsetDateTime,
  val firstName: FirstName,
  val middleNames: MiddleNames?,
  val lastName: LastName,
  val licenceNameCategory: NameFormatCategory,
  val status: Status,
  val inCustodyAtBooking: Boolean? = null,
  val inCustodyAtAssessment: Boolean? = null,
  val dossierEmailSentDate: LocalDate? = null,
  val dossierTargetDate: LocalDate? = null,
  val recallAssessmentDueDateTime: OffsetDateTime? = null,
  val assigneeUserName: FullName? = null,
)

data class RecallResponse(
  val recallId: RecallId,
  val nomsNumber: NomsNumber,
  val createdByUserId: UserId,
  val createdDateTime: OffsetDateTime,
  val lastUpdatedDateTime: OffsetDateTime,
  val firstName: FirstName,
  val middleNames: MiddleNames?,
  val lastName: LastName,
  val croNumber: CroNumber,
  val dateOfBirth: LocalDate,
  val licenceNameCategory: NameFormatCategory,
  val status: Status,
  val documents: List<Api.RecallDocument> = emptyList(),
  val lastKnownAddresses: List<Api.LastKnownAddress> = emptyList(),
  val missingDocumentsRecords: List<Api.MissingDocumentsRecord> = emptyList(),
  val notes: List<Api.Note> = emptyList(),
  val reasonsForRecall: List<ReasonForRecall> = emptyList(),
  val rescindRecords: List<Api.RescindRecord> = emptyList(),

  val additionalLicenceConditions: Boolean? = null,
  val additionalLicenceConditionsDetail: String? = null,
  val agreeWithRecall: AgreeWithRecall? = null,
  val agreeWithRecallDetail: String? = null,
  val arrestIssues: Boolean? = null,
  val arrestIssuesDetail: String? = null,
  val assessedByUserId: UserId? = null,
  val assessedByUserName: FullName? = null,
  val assignee: UserId? = null,
  val assigneeUserName: FullName? = null,
  val authorisingAssistantChiefOfficer: String? = null,
  val bookedByUserId: UserId? = null,
  val bookedByUserName: FullName? = null,
  val bookingNumber: String? = null,
  val conditionalReleaseDate: LocalDate? = null,
  val contraband: Boolean? = null,
  val contrabandDetail: String? = null,
  val currentPrison: PrisonId? = null,
  val differentNomsNumber: Boolean? = null,
  val differentNomsNumberDetail: String? = null,
  val dossierCreatedByUserId: UserId? = null,
  val dossierCreatedByUserName: FullName? = null,
  val dossierEmailSentDate: LocalDate? = null,
  val dossierTargetDate: LocalDate? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val inCustodyAtAssessment: Boolean? = null,
  val inCustodyAtBooking: Boolean? = null,
  val indexOffence: String? = null,
  val lastKnownAddressOption: LastKnownAddressOption? = null,
  val lastReleaseDate: LocalDate? = null,
  val lastReleasePrison: PrisonId? = null,
  val licenceConditionsBreached: String? = null,
  val licenceExpiryDate: LocalDate? = null,
  val localDeliveryUnit: LocalDeliveryUnit? = null,
  val localPoliceForceId: PoliceForceId? = null,
  val mappaLevel: MappaLevel? = null,
  val previousConvictionMainName: String? = null,
  val previousConvictionMainNameCategory: NameFormatCategory? = null,
  val probationOfficerEmail: String? = null,
  val probationOfficerName: String? = null,
  val probationOfficerPhoneNumber: String? = null,
  val reasonsForRecallOtherDetail: String? = null,
  val recallAssessmentDueDateTime: OffsetDateTime? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val recallLength: RecallLength? = null,
  val recallNotificationEmailSentDateTime: OffsetDateTime? = null,
  val returnedToCustodyDateTime: OffsetDateTime? = null,
  val returnedToCustodyNotificationDateTime: OffsetDateTime? = null,
  val sentenceDate: LocalDate? = null,
  val sentenceExpiryDate: LocalDate? = null,
  val sentenceLength: Api.SentenceLength? = null,
  val sentencingCourt: CourtId? = null,
  val stopByUserName: FullName? = null,
  val stopDateTime: OffsetDateTime? = null,
  val stopReason: StopReason? = null,
  val vulnerabilityDiversity: Boolean? = null,
  val vulnerabilityDiversityDetail: String? = null,
  val warrantReferenceNumber: WarrantReferenceNumber? = null,

)

class Api {
  data class Prison(val prisonId: PrisonId, val prisonName: PrisonName, val active: Boolean)

  data class SentenceLength(val years: Int, val months: Int, val days: Int)

  data class RecallDocument(
    val documentId: DocumentId,
    val category: DocumentCategory,
    val fileName: String,
    val version: Int?,
    val details: String?,
    val createdDateTime: OffsetDateTime,
    val createdByUserName: FullName
  )

  data class MissingDocumentsRecord(
    val missingDocumentsRecordId: MissingDocumentsRecordId,
    val categories: List<DocumentCategory>,
    val emailId: DocumentId,
    val emailFileName: String,
    val details: String,
    val version: Int,
    val createdByUserName: FullName,
    val createdDateTime: OffsetDateTime
  )

  data class Note(
    val noteId: NoteId,
    val subject: String,
    val details: String,
    val index: Int,
    val documentId: DocumentId? = null,
    val fileName: String? = null,
    val createdByUserName: FullName,
    val createdDateTime: OffsetDateTime,
  )

  data class RescindRecord(
    val rescindRecordId: RescindRecordId,
    val version: Int,
    val createdByUserName: FullName,
    val createdDateTime: OffsetDateTime,
    val lastUpdatedDateTime: OffsetDateTime,
    val requestDetails: String,
    val requestEmailId: DocumentId,
    val requestEmailFileName: String,
    val requestEmailReceivedDate: LocalDate,
    val approved: Boolean? = null,
    val decisionDetails: String? = null,
    val decisionEmailId: DocumentId? = null,
    val decisionEmailFileName: String? = null,
    val decisionEmailSentDate: LocalDate? = null,
  )

  data class LastKnownAddress(
    val lastKnownAddressId: LastKnownAddressId,
    val line1: String,
    val line2: String?,
    val town: String,
    val postcode: String?,
    val source: AddressSource,
    val index: Int,
    val createdByUserName: FullName,
    val createdDateTime: OffsetDateTime
  )

  data class PoliceForce(
    val id: PoliceForceId,
    val name: PoliceForceName
  )
}

data class Pdf(val content: String)

data class RecallSearchRequest(val nomsNumber: NomsNumber)

private val ALL_BANDINGS = setOf(THREE, FOUR_PLUS)
private val FOUR_PLUS_ONLY = setOf(FOUR_PLUS)

enum class Status(val visibilityBands: Set<CaseworkerBand>) {
  BEING_BOOKED_ON(ALL_BANDINGS),
  BOOKED_ON(FOUR_PLUS_ONLY),
  IN_ASSESSMENT(FOUR_PLUS_ONLY),
  ASSESSED_NOT_IN_CUSTODY(ALL_BANDINGS),
  AWAITING_RETURN_TO_CUSTODY(ALL_BANDINGS),
  AWAITING_DOSSIER_CREATION(ALL_BANDINGS),
  DOSSIER_IN_PROGRESS(ALL_BANDINGS),
  DOSSIER_ISSUED(ALL_BANDINGS),
  STOPPED(ALL_BANDINGS);
}

enum class StopReason(val label: String, val validForStopCall: Boolean = true) {
  ALTERNATIVE_INTERVENTION("Alternative intervention"),
  DECEASED("Deceased"),
  HDC_WARNING_LETTER("HDC warning letter"),
  INCORRECT_LICENCE("Incorrect licence"),
  LEGAL_REASON("Legal reason"),
  NO_ACTION("No action"),
  NOT_APPLICABLE("Not applicable"),
  NOT_SPECIFIED("Not specified"),
  RAISED_IN_ERROR("Raise in error"),
  SOS_WARNING_LETTER("SoS warning letter"),
  UO_28DAY_AND_DEEMED_NOTIFIED("UO 28 day & deemed notified"),
  WITHDRAWAL_BY_ACO("Withdrawal by ACO"),
  RESCINDED("Rescinded", false),
  POOR_BEHAVIOUR_DRUGS("Poor behaviour - Drugs"),
  POOR_BEHAVIOUR_NON_COMPLIANCE("Poor behaviour - Non-compliance"),
  POOR_BEHAVIOUR_RELATIONSHIPS("Poor behaviour - Relationships"),
  TRAVELLING_OUTSIDE_UK("Travelling outside the UK"),
  OTHER("Other")
}

data class UpdateRecallRequest(
  val licenceNameCategory: NameFormatCategory? = null,
  val lastReleasePrison: PrisonId? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForceId: PoliceForceId? = null,
  val inCustodyAtBooking: Boolean? = null,
  val inCustodyAtAssessment: Boolean? = null,
  val contraband: Boolean? = null,
  val contrabandDetail: String? = null,
  val vulnerabilityDiversity: Boolean? = null,
  val vulnerabilityDiversityDetail: String? = null,
  val mappaLevel: MappaLevel? = null,
  val sentenceDate: LocalDate? = null,
  val licenceExpiryDate: LocalDate? = null,
  val sentenceExpiryDate: LocalDate? = null,
  val sentencingCourt: CourtId? = null,
  val indexOffence: String? = null,
  val conditionalReleaseDate: LocalDate? = null,
  val sentenceLength: Api.SentenceLength? = null,
  val bookingNumber: String? = null,
  val probationOfficerName: String? = null,
  val probationOfficerPhoneNumber: String? = null,
  val probationOfficerEmail: String? = null,
  val localDeliveryUnit: LocalDeliveryUnit? = null,
  val authorisingAssistantChiefOfficer: String? = null,
  val licenceConditionsBreached: String? = null,
  val reasonsForRecall: Set<ReasonForRecall>? = null,
  val reasonsForRecallOtherDetail: String? = null,
  val agreeWithRecall: AgreeWithRecall? = null,
  val agreeWithRecallDetail: String? = null,
  val currentPrison: PrisonId? = null,
  val additionalLicenceConditions: Boolean? = null,
  val additionalLicenceConditionsDetail: String? = null,
  val differentNomsNumber: Boolean? = null,
  val differentNomsNumberDetail: String? = null,
  val recallNotificationEmailSentDateTime: OffsetDateTime? = null,
  val dossierEmailSentDate: LocalDate? = null,
  val previousConvictionMainNameCategory: NameFormatCategory? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val previousConvictionMainName: String? = null,
  val assessedByUserId: UserId? = null,
  val bookedByUserId: UserId? = null,
  val dossierCreatedByUserId: UserId? = null,
  val lastKnownAddressOption: LastKnownAddressOption? = null,
  val arrestIssues: Boolean? = null,
  val arrestIssuesDetail: String? = null,
  val warrantReferenceNumber: WarrantReferenceNumber? = null,
)

enum class RecallLength {
  FOURTEEN_DAYS,
  TWENTY_EIGHT_DAYS
}

enum class MappaLevel(val label: String) {
  NA("N/A"),
  LEVEL_1("Level 1"),
  LEVEL_2("Level 2"),
  LEVEL_3("Level 3"),
  NOT_KNOWN("Not Known"),
  CONFIRMATION_REQUIRED("Confirmation Required")
}

enum class RecallType {
  FIXED
}

enum class ReasonForRecall(val label: String) {
  BREACH_EXCLUSION_ZONE("Breach of exclusion zone"),
  ELM_BREACH_EXCLUSION_ZONE("Electronic locking and monitoring (ELM) - Breach of exclusion zone - detected by ELM"),
  ELM_BREACH_NON_CURFEW_CONDITION("Electronic locking and monitoring (ELM) - Breach of non-curfew related condition"),
  ELM_FURTHER_OFFENCE("Electronic locking and monitoring (ELM) - Charged with a further offence - detected by ELM"),
  ELM_EQUIPMENT_TAMPER("Electronic locking and monitoring (ELM) - Equipment tamper"),
  ELM_FAILURE_CHARGE_BATTERY("Electronic locking and monitoring (ELM) - Failure to charge battery"),
  FAILED_HOME_VISIT("Failed home visit"),
  FAILED_KEEP_IN_TOUCH("Failed to keep in touch"),
  FAILED_RESIDE("Failed to reside"),
  FAILED_WORK_AS_APPROVED("Failed to work as approved"),
  POOR_BEHAVIOUR_ALCOHOL("Poor behaviour - Alcohol"),
  POOR_BEHAVIOUR_FURTHER_OFFENCE("Poor behaviour - Charged with a further offence"),
  POOR_BEHAVIOUR_DRUGS("Poor behaviour - Drugs"),
  POOR_BEHAVIOUR_NON_COMPLIANCE("Poor behaviour - Non-compliance"),
  POOR_BEHAVIOUR_RELATIONSHIPS("Poor behaviour - Relationships"),
  TRAVELLING_OUTSIDE_UK("Travelling outside the UK"),
  OTHER("Other")
}

enum class AgreeWithRecall {
  YES,
  NO_STOP
}

enum class NameFormatCategory {
  FIRST_LAST,
  FIRST_MIDDLE_LAST,
  OTHER
}

enum class LastKnownAddressOption(val label: String) {
  YES("YES"),
  NO_FIXED_ABODE("No Fixed Abode")
}
