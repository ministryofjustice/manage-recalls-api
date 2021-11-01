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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.dossier.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.encodeToBase64String
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.lettertoprison.LetterToPrisonService
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.personName
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.CourtId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.DocumentId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.CourtValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.PrisonValidationService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.UserDetailsService
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallsController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val recallNotificationService: RecallNotificationService,
  @Autowired private val dossierService: DossierService,
  @Autowired private val letterToPrison: LetterToPrisonService,
  @Autowired private val userDetailsService: UserDetailsService,
  @Autowired private val recallService: RecallService,
  @Autowired private val prisonValidationService: PrisonValidationService,
  @Autowired private val courtValidationService: CourtValidationService
) {

  @PostMapping("/recalls")
  fun bookRecall(@RequestBody bookRecallRequest: BookRecallRequest): ResponseEntity<RecallResponse> =
    ResponseEntity(
      recallRepository.save(bookRecallRequest.toRecall()).toResponse(),
      HttpStatus.CREATED
    )

  @GetMapping("/recalls")
  fun findAll(): List<RecallResponse> = recallRepository.findAll().map { it.toResponse() }

  @PostMapping("/recalls/search")
  fun recallSearch(@RequestBody searchRequest: RecallSearchRequest): List<RecallResponse> =
    recallRepository.search(searchRequest).map { it.toResponse() }

  @GetMapping("/recalls/{recallId}")
  fun getRecall(@PathVariable("recallId") recallId: RecallId): RecallResponse =
    recallRepository.getByRecallId(recallId).toResponse()

  @PatchMapping("/recalls/{recallId}")
  fun updateRecall(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody updateRecallRequest: UpdateRecallRequest
  ): ResponseEntity<RecallResponse> =
    if (prisonValidationService.isValidAndActive(updateRecallRequest.currentPrison) &&
      prisonValidationService.isValid(updateRecallRequest.lastReleasePrison) &&
      courtValidationService.isValid(updateRecallRequest.sentencingCourt)
    ) {
      ResponseEntity.ok(
        recallService.updateRecall(recallId, updateRecallRequest).toResponse()
      )
    } else {
      ResponseEntity.badRequest().build()
    }

  @GetMapping("/recalls/{recallId}/recallNotification/{userId}")
  fun getRecallNotification(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("userId") userId: UserId
  ): Mono<ResponseEntity<Pdf>> =
    recallNotificationService.getDocument(recallId, userId).map {
      ResponseEntity.ok(Pdf.encode(it))
    }

  @GetMapping("/recalls/{recallId}/dossier")
  fun getDossier(@PathVariable("recallId") recallId: RecallId): Mono<ResponseEntity<Pdf>> =
    dossierService.getDossier(recallId).map {
      ResponseEntity.ok(Pdf.encode(it))
    }

  @GetMapping("/recalls/{recallId}/letter-to-prison")
  fun getLetterToPrison(@PathVariable("recallId") recallId: RecallId): Mono<ResponseEntity<Pdf>> =
    letterToPrison.getPdf(recallId).map {
      ResponseEntity.ok(Pdf.encode(it))
    }

  @PostMapping("/recalls/{recallId}/assignee/{assignee}")
  fun assignRecall(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("assignee") assignee: UserId
  ): RecallResponse =
    recallService.assignRecall(recallId, assignee).toResponse()

  @DeleteMapping("/recalls/{recallId}/assignee/{assignee}")
  fun unassignRecall(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("assignee") assignee: UserId
  ): RecallResponse =
    recallService.unassignRecall(recallId, assignee).toResponse()

  fun Recall.toResponse() = RecallResponse(
    recallId = this.recallId(),
    nomsNumber = this.nomsNumber,
    createdByUserId = this.createdByUserId(),
    createdDateTime = this.createdDateTime,
    lastUpdatedDateTime = this.lastUpdatedDateTime,
    status = this.status(),
    documents = documents.map { doc -> ApiRecallDocument(doc.id(), doc.category, doc.fileName) },
    recallLength = this.recallLength,
    lastReleasePrison = this.lastReleasePrison,
    lastReleaseDate = this.lastReleaseDate,
    recallEmailReceivedDateTime = this.recallEmailReceivedDateTime,
    recallAssessmentDueDateTime = this.recallAssessmentDueDateTime(),
    localPoliceForce = this.localPoliceForce,
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
    hasOtherPreviousConvictionMainName = this.hasOtherPreviousConvictionMainName,
    hasDossierBeenChecked = this.hasDossierBeenChecked,
    previousConvictionMainName = this.previousConvictionMainName,
    assessedByUserId = this.assessedByUserId(),
    bookedByUserId = this.bookedByUserId(),
    dossierCreatedByUserId = this.dossierCreatedByUserId(),
    dossierTargetDate = this.dossierTargetDate,
    assignee = this.assignee(),
    assigneeUserName = this.assignee()?.let { userDetailsService.find(it)?.personName() }
  )
}

fun BookRecallRequest.toRecall(): Recall {
  val now = OffsetDateTime.now()
  return Recall(::RecallId.random(), this.nomsNumber, this.createdByUserId, now, now)
}

data class BookRecallRequest(val nomsNumber: NomsNumber, val createdByUserId: UserId)

data class RecallResponse(
  val recallId: RecallId,
  val nomsNumber: NomsNumber,
  val createdByUserId: UserId,
  val createdDateTime: OffsetDateTime,
  val lastUpdatedDateTime: OffsetDateTime,
  val status: Status? = null,
  val documents: List<ApiRecallDocument> = emptyList(),
  val recallLength: RecallLength? = null,
  val lastReleasePrison: PrisonId? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForce: String? = null,
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
  val hasOtherPreviousConvictionMainName: Boolean? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val previousConvictionMainName: String? = null,
  val assessedByUserId: UserId? = null,
  val bookedByUserId: UserId? = null,
  val dossierCreatedByUserId: UserId? = null,
  val dossierTargetDate: LocalDate? = null,
  val assignee: UserId? = null,
  val assigneeUserName: String? = null,
  val recallAssessmentDueDateTime: OffsetDateTime? = null
)

class Api {
  data class SentenceLength(val years: Int, val months: Int, val days: Int)
}

data class ApiRecallDocument(
  val documentId: DocumentId,
  val category: RecallDocumentCategory,
  val fileName: String?
)

data class Pdf(val content: String) {
  companion object {
    fun encode(content: ByteArray): Pdf = Pdf(content.encodeToBase64String())
  }
}

data class RecallSearchRequest(val nomsNumber: NomsNumber)
enum class Status {
  BOOKED_ON,
  IN_ASSESSMENT,
  RECALL_NOTIFICATION_ISSUED,
  DOSSIER_IN_PROGRESS,
  DOSSIER_ISSUED
}

data class UpdateRecallRequest(
  val lastReleasePrison: PrisonId? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForce: String? = null,
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
  val hasOtherPreviousConvictionMainName: Boolean? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val previousConvictionMainName: String? = null,
  val assessedByUserId: UserId? = null,
  val bookedByUserId: UserId? = null,
  val dossierCreatedByUserId: UserId? = null
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
