package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.apache.logging.log4j.util.Strings.isBlank
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.UserId
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.random
import uk.gov.justice.digital.hmpps.managerecallsapi.service.DossierService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallDocumentService
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotFoundException
import uk.gov.justice.digital.hmpps.managerecallsapi.service.RecallNotificationService
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
  @Autowired private val recallNotificationService: RecallNotificationService,
  @Autowired private val recallDocumentService: RecallDocumentService,
  @Autowired private val dossierService: DossierService,
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

  @PostMapping("/recalls/search")
  fun recallSearch(@RequestBody searchRequest: RecallSearchRequest): List<RecallResponse> =
    recallRepository.search(searchRequest).map { it.toResponse() }

  @GetMapping("/recalls/{recallId}")
  fun getRecall(@PathVariable("recallId") recallId: RecallId): RecallResponse =
    recallRepository.getByRecallId(recallId).toResponse()

  @GetMapping("/recalls/{recallId}/recallNotification/{userId}")
  fun getRecallNotification(@PathVariable("recallId") recallId: RecallId, @PathVariable("userId") userId: UserId): Mono<ResponseEntity<Pdf>> =
    recallNotificationService.getDocument(recallId, userId)
      .map {
        ResponseEntity.ok(Pdf.encode(it))
      }

  @GetMapping("/recalls/{recallId}/dossier")
  fun getDossier(@PathVariable("recallId") recallId: RecallId): Mono<ResponseEntity<Pdf>> =
    dossierService.getDossier(recallId)
      .map {
        ResponseEntity.ok(Pdf.encode(it))
      }

  @GetMapping("/recalls/{recallId}/letter-to-prison")
  fun getLetterToPrison(@PathVariable("recallId") recallId: RecallId): Mono<ResponseEntity<Pdf>> =
    Mono.just(
      ResponseEntity.ok(
        Pdf.encode(
          "JVBERi0xLjINJeLjz9MNCjMgMCBvYmoNPDwgDS9MaW5lYXJpemVkIDEgDS9PIDUgDS9IIFsgNzYwIDE1NyBdIA0vTCAzOTA4IA0vRSAzNjU4IA0vTiAxIA0vVCAzNzMxIA0+PiANZW5kb2JqDSAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB4cmVmDTMgMTUgDTAwMDAwMDAwMTYgMDAwMDAgbg0KMDAwMDAwMDY0NCAwMDAwMCBuDQowMDAwMDAwOTE3IDAwMDAwIG4NCjAwMDAwMDEwNjggMDAwMDAgbg0KMDAwMDAwMTIyNCAwMDAwMCBuDQowMDAwMDAxNDEwIDAwMDAwIG4NCjAwMDAwMDE1ODkgMDAwMDAgbg0KMDAwMDAwMTc2OCAwMDAwMCBuDQowMDAwMDAyMTk3IDAwMDAwIG4NCjAwMDAwMDIzODMgMDAwMDAgbg0KMDAwMDAwMjc2OSAwMDAwMCBuDQowMDAwMDAzMTcyIDAwMDAwIG4NCjAwMDAwMDMzNTEgMDAwMDAgbg0KMDAwMDAwMDc2MCAwMDAwMCBuDQowMDAwMDAwODk3IDAwMDAwIG4NCnRyYWlsZXINPDwNL1NpemUgMTgNL0luZm8gMSAwIFIgDS9Sb290IDQgMCBSIA0vUHJldiAzNzIyIA0vSURbPGQ3MGY0NmM1YmE0ZmU4YmQ0OWE5ZGQwNTk5YjBiMTUxPjxkNzBmNDZjNWJhNGZlOGJkNDlhOWRkMDU5OWIwYjE1MT5dDT4+DXN0YXJ0eHJlZg0wDSUlRU9GDSAgICAgIA00IDAgb2JqDTw8IA0vVHlwZSAvQ2F0YWxvZyANL1BhZ2VzIDIgMCBSIA0vT3BlbkFjdGlvbiBbIDUgMCBSIC9YWVogbnVsbCBudWxsIG51bGwgXSANL1BhZ2VNb2RlIC9Vc2VOb25lIA0+PiANZW5kb2JqDTE2IDAgb2JqDTw8IC9TIDM2IC9GaWx0ZXIgL0ZsYXRlRGVjb2RlIC9MZW5ndGggMTcgMCBSID4+IA1zdHJlYW0NCkiJYmBg4GVgYPrBAAScFxiwAQ4oLQDE3FDMwODHwKkyubctWLfmpsmimQ5AEYAAAwC3vwe0DWVuZHN0cmVhbQ1lbmRvYmoNMTcgMCBvYmoNNTMgDWVuZG9iag01IDAgb2JqDTw8IA0vVHlwZSAvUGFnZSANL1BhcmVudCAyIDAgUiANL1Jlc291cmNlcyA2IDAgUiANL0NvbnRlbnRzIDEwIDAgUiANL01lZGlhQm94IFsgMCAwIDYxMiA3OTIgXSANL0Nyb3BCb3ggWyAwIDAgNjEyIDc5MiBdIA0vUm90YXRlIDAgDT4+IA1lbmRvYmoNNiAwIG9iag08PCANL1Byb2NTZXQgWyAvUERGIC9UZXh0IF0gDS9Gb250IDw8IC9UVDIgOCAwIFIgL1RUNCAxMiAwIFIgL1RUNiAxMyAwIFIgPj4gDS9FeHRHU3RhdGUgPDwgL0dTMSAxNSAwIFIgPj4gDS9Db2xvclNwYWNlIDw8IC9DczUgOSAwIFIgPj4gDT4+IA1lbmRvYmoNNyAwIG9iag08PCANL1R5cGUgL0ZvbnREZXNjcmlwdG9yIA0vQXNjZW50IDg5MSANL0NhcEhlaWdodCAwIA0vRGVzY2VudCAtMjE2IA0vRmxhZ3MgMzQgDS9Gb250QkJveCBbIC01NjggLTMwNyAyMDI4IDEwMDcgXSANL0ZvbnROYW1lIC9UaW1lc05ld1JvbWFuIA0vSXRhbGljQW5nbGUgMCANL1N0ZW1WIDAgDT4+IA1lbmRvYmoNOCAwIG9iag08PCANL1R5cGUgL0ZvbnQgDS9TdWJ0eXBlIC9UcnVlVHlwZSANL0ZpcnN0Q2hhciAzMiANL0xhc3RDaGFyIDMyIA0vV2lkdGhzIFsgMjUwIF0gDS9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIA0vQmFzZUZvbnQgL1RpbWVzTmV3Um9tYW4gDS9Gb250RGVzY3JpcHRvciA3IDAgUiANPj4gDWVuZG9iag05IDAgb2JqDVsgDS9DYWxSR0IgPDwgL1doaXRlUG9pbnQgWyAwLjk1MDUgMSAxLjA4OSBdIC9HYW1tYSBbIDIuMjIyMjEgMi4yMjIyMSAyLjIyMjIxIF0gDS9NYXRyaXggWyAwLjQxMjQgMC4yMTI2IDAuMDE5MyAwLjM1NzYgMC43MTUxOSAwLjExOTIgMC4xODA1IDAuMDcyMiAwLjk1MDUgXSA+PiANDV0NZW5kb2JqDTEwIDAgb2JqDTw8IC9MZW5ndGggMzU1IC9GaWx0ZXIgL0ZsYXRlRGVjb2RlID4+IA1zdHJlYW0NCkiJdJDBTsMwEETv/oo92ohuvXHsJEeggOCEwDfEIU1SCqIJIimIv2dthyJVQpGc0Xo88+xzL5beZ0DgN4IIq6oCzd8sK43amAyK3GKmTQV+J5YXo4VmjDYNYyOW1w8Ez6PQ4JuwfAkJyr+yXNgSSwt+NU+4Kp+rcg4uy9Q1a6MdarLcpgvUeUGh7RBFSLk1f1n+5FgsHJaZttFqA+tKLJhfZ3kEY+VcoHuUfvui2O3kCL9COSwk1Ok3deMEd6srUCVa2Q7Nftf1Ewar5a4nfxuu4v59NcLMGAKXlcjMLtwj1BsTQCITUSK52cC3IoNGDnto6l5VmEv4YAwjO8VWJ+s2DSeGttw/qmA/PZyLu3vY1p9p0MGZIs2iHdZxjwdNSkzedT0pJiW+CWl5H0O7uu2SB1JLn8rHlMkH2F+/xa20Rjp+nAQ39Ec8c1gz7KJ4T3H7uXnuwvSWl178CDAA/bGPlAplbmRzdHJlYW0NZW5kb2JqDTExIDAgb2JqDTw8IA0vVHlwZSAvRm9udERlc2NyaXB0b3IgDS9Bc2NlbnQgOTA1IA0vQ2FwSGVpZ2h0IDAgDS9EZXNjZW50IC0yMTEgDS9GbGFncyAzMiANL0ZvbnRCQm94IFsgLTYyOCAtMzc2IDIwMzQgMTA0OCBdIA0vRm9udE5hbWUgL0FyaWFsLEJvbGQgDS9JdGFsaWNBbmdsZSAwIA0vU3RlbVYgMTMzIA0+PiANZW5kb2JqDTEyIDAgb2JqDTw8IA0vVHlwZSAvRm9udCANL1N1YnR5cGUgL1RydWVUeXBlIA0vRmlyc3RDaGFyIDMyIA0vTGFzdENoYXIgMTE3IA0vV2lkdGhzIFsgMjc4IDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMjc4IDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgDTAgMCAwIDAgMCA3MjIgMCA2MTEgMCAwIDAgMCAwIDAgMCAwIDAgNjY3IDAgMCAwIDYxMSAwIDAgMCAwIDAgMCANMCAwIDAgMCAwIDAgNTU2IDAgNTU2IDYxMSA1NTYgMCAwIDYxMSAyNzggMCAwIDAgODg5IDYxMSA2MTEgMCAwIA0wIDU1NiAzMzMgNjExIF0gDS9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIA0vQmFzZUZvbnQgL0FyaWFsLEJvbGQgDS9Gb250RGVzY3JpcHRvciAxMSAwIFIgDT4+IA1lbmRvYmoNMTMgMCBvYmoNPDwgDS9UeXBlIC9Gb250IA0vU3VidHlwZSAvVHJ1ZVR5cGUgDS9GaXJzdENoYXIgMzIgDS9MYXN0Q2hhciAxMjEgDS9XaWR0aHMgWyAyNzggMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDI3OCAwIDI3OCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMCAwIDAgMCANMCAwIDAgNjY3IDAgMCAwIDAgMCAwIDAgMjc4IDAgMCAwIDAgMCAwIDAgMCA3MjIgMCAwIDAgMCAwIDAgMCAwIA0wIDAgMCAwIDAgMCA1NTYgNTU2IDUwMCA1NTYgNTU2IDI3OCAwIDU1NiAyMjIgMCAwIDIyMiA4MzMgNTU2IDU1NiANNTU2IDAgMzMzIDUwMCAyNzggNTU2IDUwMCAwIDAgNTAwIF0gDS9FbmNvZGluZyAvV2luQW5zaUVuY29kaW5nIA0vQmFzZUZvbnQgL0FyaWFsIA0vRm9udERlc2NyaXB0b3IgMTQgMCBSIA0+PiANZW5kb2JqDTE0IDAgb2JqDTw8IA0vVHlwZSAvRm9udERlc2NyaXB0b3IgDS9Bc2NlbnQgOTA1IA0vQ2FwSGVpZ2h0IDAgDS9EZXNjZW50IC0yMTEgDS9GbGFncyAzMiANL0ZvbnRCQm94IFsgLTY2NSAtMzI1IDIwMjggMTAzNyBdIA0vRm9udE5hbWUgL0FyaWFsIA0vSXRhbGljQW5nbGUgMCANL1N0ZW1WIDAgDT4+IA1lbmRvYmoNMTUgMCBvYmoNPDwgDS9UeXBlIC9FeHRHU3RhdGUgDS9TQSBmYWxzZSANL1NNIDAuMDIgDS9UUiAvSWRlbnRpdHkgDT4+IA1lbmRvYmoNMSAwIG9iag08PCANL1Byb2R1Y2VyIChBY3JvYmF0IERpc3RpbGxlciA0LjA1IGZvciBXaW5kb3dzKQ0vQ3JlYXRvciAoTWljcm9zb2Z0IFdvcmQgOS4wKQ0vTW9kRGF0ZSAoRDoyMDAxMDgyOTA5NTUwMS0wNycwMCcpDS9BdXRob3IgKEdlbmUgQnJ1bWJsYXkpDS9UaXRsZSAoVGhpcyBpcyBhIHRlc3QgUERGIGRvY3VtZW50KQ0vQ3JlYXRpb25EYXRlIChEOjIwMDEwODI5MDk1NDU3KQ0+PiANZW5kb2JqDTIgMCBvYmoNPDwgDS9UeXBlIC9QYWdlcyANL0tpZHMgWyA1IDAgUiBdIA0vQ291bnQgMSANPj4gDWVuZG9iag14cmVmDTAgMyANMDAwMDAwMDAwMCA2NTUzNSBmDQowMDAwMDAzNDI5IDAwMDAwIG4NCjAwMDAwMDM2NTggMDAwMDAgbg0KdHJhaWxlcg08PA0vU2l6ZSAzDS9JRFs8ZDcwZjQ2YzViYTRmZThiZDQ5YTlkZDA1OTliMGIxNTE+PGQ3MGY0NmM1YmE0ZmU4YmQ0OWE5ZGQwNTk5YjBiMTUxPl0NPj4Nc3RhcnR4cmVmDTE3Mw0lJUVPRg0=".toByteArray()
        )
      )
    )

  @PostMapping("/recalls/{recallId}/documents")
  fun addDocument(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody body: AddDocumentRequest
  ): ResponseEntity<AddDocumentResponse> {
    // TODO:  Restrict the types of documents that can be uploaded. i.e. RECALL_NOTIFICATION, REVOCATION_ORDER
    val documentId = try {
      recallDocumentService.uploadAndAddDocumentForRecall(
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
  dossierCreatedByUserId = this.dossierCreatedByUserId()
)

data class BookRecallRequest(val nomsNumber: NomsNumber)

data class RecallResponse(
  val recallId: RecallId,
  val nomsNumber: NomsNumber,
  val documents: List<ApiRecallDocument> = emptyList(),
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
  val localDeliveryUnit: LocalDeliveryUnit? = null,
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
  val recallNotificationEmailSentDateTime: OffsetDateTime? = null,
  val dossierEmailSentDate: LocalDate? = null,
  val hasOtherPreviousConvictionMainName: Boolean? = null,
  val hasDossierBeenChecked: Boolean? = null,
  val previousConvictionMainName: String? = null,
  val assessedByUserId: UserId? = null,
  val bookedByUserId: UserId? = null,
  val dossierCreatedByUserId: UserId? = null,
) {
  val status: Status? = calculateStatus()

  private fun calculateStatus(): Status? {

    // TODO PUD-616 debug: remove ASAP: logging here in order to be adjacent to sending response for all RecallResponses
    val log = LoggerFactory.getLogger(this::class.java)
    val lduIsNotBlankWhenOneOrMoreOtherProbationPropertyIsBlank = localDeliveryUnit != null &&
      (
        isBlank(probationOfficerName) || isBlank(probationOfficerEmail) || isBlank(probationOfficerPhoneNumber) || isBlank(
          authorisingAssistantChiefOfficer
        )
        )
    log.info("PUD-616 temporary debug logging: LDU set when other ProbationInfo is not = $lduIsNotBlankWhenOneOrMoreOtherProbationPropertyIsBlank, localDeliveryUnit = $localDeliveryUnit, recallId = $recallId")
    // TODO remove above ASAP

    return if (recallNotificationEmailSentDateTime != null) {
      Status.RECALL_NOTIFICATION_ISSUED
    } else if (bookedByUserId != null) {
      Status.BOOKED_ON
    } else {
      null
    }
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
    fun encode(content: ByteArray): Pdf = Pdf(Base64.getEncoder().encodeToString(content))
  }
}

data class AddDocumentRequest(
  val category: RecallDocumentCategory,
  val fileContent: String,
  val fileName: String? = null
)

data class AddDocumentResponse(val documentId: UUID)

data class RecallSearchRequest(val nomsNumber: NomsNumber)

data class GetDocumentResponse(
  val documentId: UUID,
  val category: RecallDocumentCategory,
  val content: String,
  val fileName: String?
)

enum class Status {
  RECALL_NOTIFICATION_ISSUED,
  BOOKED_ON
}
