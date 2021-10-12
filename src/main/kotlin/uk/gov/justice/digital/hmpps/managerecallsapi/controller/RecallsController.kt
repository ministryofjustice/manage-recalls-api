package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
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
import uk.gov.justice.digital.hmpps.managerecallsapi.documents.recallnotification.RecallNotificationService
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.PrisonId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallsController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val recallNotificationService: RecallNotificationService,
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Autowired private val dossierService: DossierService,
  @Autowired private val letterToPrison: LetterToPrisonService
) {

  @PostMapping("/recalls")
  fun bookRecall(@RequestBody bookRecallRequest: BookRecallRequest) =
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

  @GetMapping("/recalls/{recallId}/recallNotification/{userId}")
  fun getRecallNotification(@PathVariable("recallId") recallId: RecallId, @PathVariable("userId") userId: UserId): Mono<ResponseEntity<Pdf>> =
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

  @GetMapping("/recalls/{recallId}/documents/{documentId}")
  fun getRecallDocument(
    @PathVariable("recallId") recallId: RecallId,
    @PathVariable("documentId") documentId: UUID
  ): ResponseEntity<GetDocumentResponse> {
    val (document, bytes) = recallDocumentService.getDocument(recallId, documentId)
    return ResponseEntity.ok(
      GetDocumentResponse(
        documentId = documentId,
        category = document.category,
        content = bytes.encodeToBase64String(),
        fileName = document.fileName
      )
    )
  }
}

fun BookRecallRequest.toRecall() = Recall(::RecallId.random(), this.nomsNumber)

fun Recall.toResponse() = RecallResponse(
  recallId = this.recallId(),
  nomsNumber = this.nomsNumber,
  documents = this.documents.map { doc -> ApiRecallDocument(doc.id, doc.category, doc.fileName) },
  recallLength = this.recallLength,
  lastReleasePrison = this.lastReleasePrison,
  lastReleaseDate = this.lastReleaseDate,
  recallEmailReceivedDateTime = this.recallEmailReceivedDateTime,
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
  sentenceLength = this.sentencingInfo?.sentenceLength?.let { Api.SentenceLength(it.sentenceYears, it.sentenceMonths, it.sentenceDays) },
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
  dossierTargetDate = this.dossierTargetDate
)

data class BookRecallRequest(val nomsNumber: NomsNumber)

data class RecallResponse(
  val recallId: RecallId,
  val nomsNumber: NomsNumber,
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
  val sentencingCourt: String? = null,
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
  val dossierTargetDate: LocalDate? = null
) {
  val recallAssessmentDueDateTime: OffsetDateTime? = recallEmailReceivedDateTime?.plusHours(24)
  val status: Status? = calculateStatus()

  private fun calculateStatus(): Status? =
    if (dossierCreatedByUserId != null) {
      Status.DOSSIER_ISSUED
    } else if (recallNotificationEmailSentDateTime != null) {
      Status.RECALL_NOTIFICATION_ISSUED
    } else if (bookedByUserId != null) {
      Status.BOOKED_ON
    } else {
      null
    }
}

class Api {
  data class SentenceLength(val years: Int, val months: Int, val days: Int)
}

data class ApiRecallDocument(
  val documentId: UUID,
  val category: RecallDocumentCategory,
  val fileName: String?
)

data class Pdf(val content: String) {
  companion object {
    fun encode(content: ByteArray): Pdf = Pdf(content.encodeToBase64String())
  }
}

data class RecallSearchRequest(val nomsNumber: NomsNumber)

data class GetDocumentResponse(
  val documentId: UUID,
  val category: RecallDocumentCategory,
  val content: String,
  val fileName: String?
)

enum class Status {
  RECALL_NOTIFICATION_ISSUED,
  BOOKED_ON,
  DOSSIER_ISSUED
}
