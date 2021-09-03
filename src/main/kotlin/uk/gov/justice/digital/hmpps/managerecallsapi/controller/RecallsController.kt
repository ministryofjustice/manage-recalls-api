package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallDocumentCategory
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.NomsNumber
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RevocationOrderService
import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class RecallsController(
  @Autowired private val recallRepository: RecallRepository,
  @Autowired private val revocationOrderService: RevocationOrderService,
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Value("\${manage-recalls-api.base-uri}") private val baseUri: String
) {

  @PostMapping("/recalls")
  fun bookRecall(@RequestBody bookRecallRequest: BookRecallRequest) =
    ResponseEntity(
      recallRepository.save(bookRecallRequest.toRecall()).toResponse(),
      HttpStatus.CREATED
    )

  @GetMapping("/recalls")
  fun findAll(): List<RecallResponse> = recallRepository.findAll().map { it.toResponse() }

  @GetMapping("/recalls/{recallId}")
  fun getRecall(@PathVariable("recallId") recallId: RecallId): RecallResponse =
    recallRepository.getByRecallId(recallId).toResponse()

  @GetMapping("/recalls/{recallId}/revocationOrder")
  fun getRevocationOrder(@PathVariable("recallId") recallId: RecallId): Mono<ResponseEntity<Pdf>> =
    revocationOrderService.getRevocationOrder(recallId)
      .map {
        val pdfBase64Encoded = Base64.getEncoder().encodeToString(it)
        ResponseEntity.ok(Pdf(pdfBase64Encoded))
      }

  @PostMapping("/recalls/{recallId}/documents")
  fun addDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody body: AddDocumentRequest
  ): ResponseEntity<AddDocumentResponse> {
    val documentId = try {
      recallDocumentService.addDocumentToRecall(
        recallId = recallId,
        documentBytes = Base64.getDecoder().decode(body.fileContent),
        documentCategory = body.category,
        fileName = body.fileName
      )
    } catch (e: RecallNotFoundException) {
      throw ResponseStatusException(BAD_REQUEST, e.message, e)
    }

    return ResponseEntity
      .created(URI.create("$baseUri/recalls/$recallId/documents/$documentId"))
      .body(AddDocumentResponse(documentId = documentId))
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
        content = Base64.getEncoder().encodeToString(bytes),
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
  revocationOrderId = this.revocationOrderId,
  recallLength = this.recallLength,
  lastReleasePrison = this.lastReleasePrison,
  lastReleaseDate = this.lastReleaseDate,
  recallEmailReceivedDateTime = this.recallEmailReceivedDateTime,
  localPoliceForce = localPoliceForce,
  vulnerabilityDiversityDetail = this.vulnerabilityDiversityDetail,
  contrabandDetail = this.contrabandDetail,
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
  probationDivision = this.probationInfo?.probationDivision,
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
  recallNotificationEmailSentDateTime = this.recallNotificationEmailSentDateTime
)

data class BookRecallRequest(val nomsNumber: NomsNumber)

data class RecallResponse(
  val recallId: RecallId,
  val nomsNumber: NomsNumber,
  val documents: List<ApiRecallDocument> = emptyList(),
  val revocationOrderId: UUID? = null,
  val recallLength: RecallLength? = null,
  val lastReleasePrison: String? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForce: String? = null,
  val vulnerabilityDiversityDetail: String? = null,
  val contrabandDetail: String? = null,
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
  val probationDivision: ProbationDivision? = null,
  val authorisingAssistantChiefOfficer: String? = null,
  val licenceConditionsBreached: String? = null,
  val reasonsForRecall: List<ReasonForRecall> = emptyList(),
  val reasonsForRecallOtherDetail: String? = null,
  val agreeWithRecall: AgreeWithRecall? = null,
  val agreeWithRecallDetail: String? = null,
  val currentPrison: String? = null,
  val additionalLicenceConditions: Boolean? = null,
  val additionalLicenceConditionsDetail: String? = null,
  val differentNomsNumber: Boolean? = null,
  val differentNomsNumberDetail: String? = null,
  val recallNotificationEmailSentDateTime: OffsetDateTime? = null
)

class Api {
  data class SentenceLength(val years: Int, val months: Int, val days: Int)
}

data class ApiRecallDocument(
  val documentId: UUID,
  val category: RecallDocumentCategory,
  val fileName: String?
)

data class Pdf(val content: String)

data class AddDocumentRequest(val category: RecallDocumentCategory, val fileContent: String, val fileName: String?)

data class AddDocumentResponse(val documentId: UUID)

data class GetDocumentResponse(
  val documentId: UUID,
  val category: RecallDocumentCategory,
  val content: String,
  val fileName: String?
)
