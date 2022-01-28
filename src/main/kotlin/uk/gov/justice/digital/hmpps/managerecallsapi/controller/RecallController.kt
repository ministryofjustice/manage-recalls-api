package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.extractor.TokenExtractor
import uk.gov.justice.digital.hmpps.managerecallsapi.db.AddressSource
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand.FOUR_PLUS
import uk.gov.justice.digital.hmpps.managerecallsapi.db.CaseworkerBand.THREE
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Document
import uk.gov.justice.digital.hmpps.managerecallsapi.db.DocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.LastKnownAddress
import uk.gov.justice.digital.hmpps.managerecallsapi.db.MissingDocumentsRecord
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FirstName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.FullName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastKnownAddressId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.LastName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MiddleNames
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.MissingDocumentsRecordId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PoliceForceName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonName
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.WarrantReferenceNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val userDetailsService: UserDetailsService,
  @Autowired private val recallService: RecallService,
  @Autowired private val documentService: DocumentService,
  @Autowired private val prisonValidationService: PrisonValidationService,
  @Autowired private val courtValidationService: CourtValidationService,
  @Autowired private val tokenExtractor: TokenExtractor
) {

  @PostMapping("/recalls")
  fun bookRecall(
    @RequestBody bookRecallRequest: BookRecallRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): ResponseEntity<RecallResponse> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)

    return ResponseEntity(
      recallRepository.save(bookRecallRequest.toRecall(token.userUuid()), token.userUuid()).toResponse(),
      HttpStatus.CREATED
    )
  }

  @GetMapping("/recalls")
  fun findAll(@RequestHeader("Authorization") bearerToken: String): List<RecallResponse> {
    val token = tokenExtractor.getTokenFromHeader(bearerToken)
    val band = userDetailsService.get(token.userUuid()).caseworkerBand

    return recallRepository.findAll().filter { it.status().visibilityBands.contains(band) }.map { it.toResponse() }
  }

  @PostMapping("/recalls/search")
  fun recallSearch(@RequestBody searchRequest: RecallSearchRequest): List<RecallResponse> =
    recallRepository.search(searchRequest).map { it.toResponse() }

  @GetMapping("/recalls/{recallId}")
  fun getRecall(@PathVariable("recallId") recallId: RecallId): RecallResponse =
    recallRepository.getByRecallId(recallId).toResponse()

  @PatchMapping("/recalls/{recallId}")
  fun updateRecall(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody updateRecallRequest: UpdateRecallRequest,
    @RequestHeader("Authorization") bearerToken: String
  ): ResponseEntity<RecallResponse> =
    if (prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) &&
      prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) &&
      courtValidationService.isValid(updateRecallRequest.sentencingCourt)
    ) {
      val token = tokenExtractor.getTokenFromHeader(bearerToken)
      ResponseEntity.ok(
        recallService.updateRecall(recallId, updateRecallRequest, token.userUuid()).toResponse()
      )
    } else {
      ResponseEntity.badRequest().build()
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

  fun Recall.toResponse() = RecallResponse(
    recallId = this.recallId(),
    nomsNumber = this.nomsNumber,
    createdByUserId = this.createdByUserId(),
    createdDateTime = this.createdDateTime,
    lastUpdatedDateTime = this.lastUpdatedDateTime,
    firstName = this.firstName,
    middleNames = this.middleNames,
    lastName = this.lastName,
    licenceNameCategory = this.licenceNameCategory,
    status = this.status(),
    documents = latestDocuments(documents),
    missingDocumentsRecords = missingDocumentsRecords.map { record -> record.toResponse() },
    lastKnownAddresses = lastKnownAddresses.map { address -> address.toResponse() },
    recallLength = this.recallLength,
    lastReleasePrison = this.lastReleasePrison,
    lastReleaseDate = this.lastReleaseDate,
    recallEmailReceivedDateTime = this.recallEmailReceivedDateTime,
    localPoliceForceId = this.localPoliceForceId,
    inCustody = this.inCustody,
    contraband = this.contraband,
    contrabandDetail = this.contrabandDetail,
    vulnerabilityDiversity = this.vulnerabilityDiversity,
    vulnerabilityDiversityDetail = this.vulnerabilityDiversityDetail,
    mappaLevel = this.mappaLevel,
    sentenceDate = this.sentencingInfo?.sentenceDate,
    licenceExpiryDate = this.sentencingInfo?.licenceExpiryDate,
    sentenceExpiryDate = this.sentencingInfo?.sentenceExpiryDate,
    sentencingCourt = this.sentencingInfo?.sentencingCourt,
    indexOffence = this.sentencingInfo?.indexOffence,
    conditionalReleaseDate = this.sentencingInfo?.conditionalReleaseDate,
    sentenceLength = this.sentencingInfo?.sentenceLength?.let {
      Api.SentenceLength(
        it.sentenceYears,
        it.sentenceMonths,
        it.sentenceDays
      )
    },
    bookingNumber = this.bookingNumber,
    probationOfficerName = this.probationInfo?.probationOfficerName,
    probationOfficerPhoneNumber = this.probationInfo?.probationOfficerPhoneNumber,
    probationOfficerEmail = this.probationInfo?.probationOfficerEmail,
    localDeliveryUnit = this.probationInfo?.localDeliveryUnit,
    authorisingAssistantChiefOfficer = this.probationInfo?.authorisingAssistantChiefOfficer,
    licenceConditionsBreached = this.licenceConditionsBreached,
    reasonsForRecall = this.reasonsForRecall.toList(),
    reasonsForRecallOtherDetail = this.reasonsForRecallOtherDetail,
    agreeWithRecall = this.agreeWithRecall,
    agreeWithRecallDetail = this.agreeWithRecallDetail,
    currentPrison = this.currentPrison,
    additionalLicenceConditions = this.additionalLicenceConditions,
    additionalLicenceConditionsDetail = this.additionalLicenceConditionsDetail,
    differentNomsNumber = this.differentNomsNumber,
    differentNomsNumberDetail = this.differentNomsNumberDetail,
    recallNotificationEmailSentDateTime = this.recallNotificationEmailSentDateTime,
    dossierEmailSentDate = this.dossierEmailSentDate,
    previousConvictionMainNameCategory = this.previousConvictionMainNameCategory,
    hasDossierBeenChecked = this.hasDossierBeenChecked,
    previousConvictionMainName = this.previousConvictionMainName,
    assessedByUserId = this.assessedByUserId(),
    bookedByUserId = this.bookedByUserId(),
    dossierCreatedByUserId = this.dossierCreatedByUserId(),
    dossierTargetDate = this.dossierTargetDate,
    assignee = this.assignee(),
    assigneeUserName = this.assignee()?.let { userDetailsService.get(it).fullName() },
    recallAssessmentDueDateTime = this.recallAssessmentDueDateTime(),
    assessedByUserName = this.assessedByUserId()?.let { userDetailsService.get(it).fullName() },
    bookedByUserName = this.bookedByUserId()?.let { userDetailsService.get(it).fullName() },
    dossierCreatedByUserName = this.dossierCreatedByUserId()?.let { userDetailsService.get(it).fullName() },
    lastKnownAddressOption = this.lastKnownAddressOption,
    arrestIssues = this.arrestIssues,
    arrestIssuesDetail = this.arrestIssuesDetail,
    warrantReferenceNumber = this.warrantReferenceNumber,
  )

  private fun latestDocuments(documents: Set<Document>): List<Api.RecallDocument> {
    val partitionedDocs = documents.partition { it.category.versioned }
    val latestDocuments = partitionedDocs.first.filter { it.category.versioned }
      .groupBy { it.category }.values.map { it.sortedBy { d -> d.version }.last() } + partitionedDocs.second
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

  fun MissingDocumentsRecord.toResponse() = Api.MissingDocumentsRecord(
    this.id(),
    this.categories.toList(),
    this.emailId(),
    documentService.getRecallDocumentById(this.recallId(), this.emailId()).fileName,
    this.details,
    this.version,
    userDetailsService.get(this.createdByUserId()).fullName(),
    this.createdDateTime
  )

  fun LastKnownAddress.toResponse() = Api.LastKnownAddress(
    this.id(),
    this.line1,
    this.line2,
    this.town,
    this.postcode,
    this.source,
    this.index,
    userDetailsService.get(this.createdByUserId()).fullName(),
    this.createdDateTime
  )
}

fun BookRecallRequest.toRecall(userUuid: UserId): Recall {
  val now = OffsetDateTime.now()
  return Recall(
    ::RecallId.random(),
    this.nomsNumber,
    userUuid,
    now,
    this.firstName,
    this.middleNames,
    this.lastName
  )
}

data class BookRecallRequest(
  val nomsNumber: NomsNumber,
  val firstName: FirstName,
  val middleNames: MiddleNames?,
  val lastName: LastName
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
  val licenceNameCategory: NameFormatCategory,
  val status: Status,
  val documents: List<Api.RecallDocument> = emptyList(),
  val missingDocumentsRecords: List<Api.MissingDocumentsRecord> = emptyList(),
  val lastKnownAddresses: List<Api.LastKnownAddress> = emptyList(),
  val recallLength: RecallLength? = null,
  val lastReleasePrison: PrisonId? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForceId: PoliceForceId? = null,
  val inCustody: Boolean? = null,
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
  val reasonsForRecall: List<ReasonForRecall> = emptyList(),
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
  val dossierTargetDate: LocalDate? = null,
  val assignee: UserId? = null,
  val assigneeUserName: FullName? = null,
  val recallAssessmentDueDateTime: OffsetDateTime? = null,
  val assessedByUserName: FullName? = null,
  val bookedByUserName: FullName? = null,
  val dossierCreatedByUserName: FullName? = null,
  val lastKnownAddressOption: LastKnownAddressOption? = null,
  val arrestIssues: Boolean? = null,
  val arrestIssuesDetail: String? = null,
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
  RECALL_NOTIFICATION_ISSUED(ALL_BANDINGS),
  DOSSIER_IN_PROGRESS(ALL_BANDINGS),
  DOSSIER_ISSUED(ALL_BANDINGS),
  STOPPED(ALL_BANDINGS);
}

data class UpdateRecallRequest(
  val licenceNameCategory: NameFormatCategory? = null,
  val lastReleasePrison: PrisonId? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForceId: PoliceForceId? = null,
  val inCustody: Boolean? = null,
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

@Suppress("unused")
enum class ReasonForRecall {
  BREACH_EXCLUSION_ZONE,
  ELM_BREACH_EXCLUSION_ZONE,
  ELM_BREACH_NON_CURFEW_CONDITION,
  ELM_FURTHER_OFFENCE,
  ELM_EQUIPMENT_TAMPER,
  ELM_FAILURE_CHARGE_BATTERY,
  FAILED_HOME_VISIT,
  FAILED_KEEP_IN_TOUCH,
  FAILED_RESIDE,
  FAILED_WORK_AS_APPROVED,
  POOR_BEHAVIOUR_ALCOHOL,
  POOR_BEHAVIOUR_FURTHER_OFFENCE,
  POOR_BEHAVIOUR_DRUGS,
  POOR_BEHAVIOUR_NON_COMPLIANCE,
  POOR_BEHAVIOUR_RELATIONSHIPS,
  TRAVELLING_OUTSIDE_UK,
  OTHER
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

enum class LastKnownAddressOption {
  YES,
  NO_FIXED_ABODE
}
