package uk.gov.justice.digital.hmpps.managerecallsapi.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.managerecallsapi.controller.RecallType.FIXED
import uk.gov.justice.digital.hmpps.managerecallsapi.db.ProbationInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.db.Recall
import uk.gov.justice.digital.hmpps.managerecallsapi.db.RecallRepository
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentenceLength
import uk.gov.justice.digital.hmpps.managerecallsapi.db.SentencingInfo
import uk.gov.justice.digital.hmpps.managerecallsapi.domain.RecallId
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
@RequestMapping(produces = [APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_MANAGE_RECALLS')")
class UpdateRecallController(
  @Autowired private val recallRepository: RecallRepository
) {

  @PatchMapping("/recalls/{recallId}")
  fun updateRecall(
    @PathVariable("recallId") recallId: RecallId,
    @RequestBody updateRecallRequest: UpdateRecallRequest
  ): ResponseEntity<RecallResponse> =
    ResponseEntity.ok(
      recallRepository.getByRecallId(recallId).let { recall ->
        val sentencingInfo = updateRecallRequest.toSentencingInfo(recall)
        recall.copy(
          recallType = FIXED,
          agreeWithRecallRecommendation = updateRecallRequest.agreeWithRecallRecommendation ?: recall.agreeWithRecallRecommendation,
          recallLength = sentencingInfo?.calculateRecallLength() ?: recall.recallLength,
          recallEmailReceivedDateTime = updateRecallRequest.recallEmailReceivedDateTime ?: recall.recallEmailReceivedDateTime,
          lastReleasePrison = updateRecallRequest.lastReleasePrison ?: recall.lastReleasePrison,
          lastReleaseDate = updateRecallRequest.lastReleaseDate ?: recall.lastReleaseDate,
          localPoliceForce = updateRecallRequest.localPoliceForce ?: recall.localPoliceForce,
          contrabandDetail = updateRecallRequest.contrabandDetail ?: recall.contrabandDetail,
          vulnerabilityDiversityDetail = updateRecallRequest.vulnerabilityDiversityDetail ?: recall.vulnerabilityDiversityDetail,
          mappaLevel = updateRecallRequest.mappaLevel ?: recall.mappaLevel,
          sentencingInfo = sentencingInfo,
          probationInfo = updateRecallRequest.toProbationInfo(recall),
          bookingNumber = updateRecallRequest.bookingNumber ?: recall.bookingNumber,
          currentPrison = updateRecallRequest.currentPrison ?: recall.currentPrison
        )
      }.let(recallRepository::save).toResponse()
    )

  private fun UpdateRecallRequest.toSentencingInfo(
    existingRecall: Recall
  ) = if (sentenceDate != null &&
    licenceExpiryDate != null &&
    sentenceExpiryDate != null &&
    sentencingCourt != null &&
    indexOffence != null &&
    sentenceLength != null
  ) SentencingInfo(
    sentenceDate,
    licenceExpiryDate,
    sentenceExpiryDate,
    sentencingCourt,
    indexOffence,
    SentenceLength(sentenceLength.years, sentenceLength.months, sentenceLength.days),
    conditionalReleaseDate
  ) else existingRecall.sentencingInfo

  private fun UpdateRecallRequest.toProbationInfo(
    existingRecall: Recall
  ) = if (
    probationOfficerName != null &&
    probationOfficerPhoneNumber != null &&
    probationOfficerEmail != null &&
    probationDivision != null &&
    authorisingAssistantChiefOfficer != null

  ) ProbationInfo(
    probationOfficerName,
    probationOfficerPhoneNumber,
    probationOfficerEmail,
    probationDivision,
    authorisingAssistantChiefOfficer,
  ) else existingRecall.probationInfo
}

data class UpdateRecallRequest(
  val agreeWithRecallRecommendation: Boolean? = null,
  val lastReleasePrison: String? = null,
  val lastReleaseDate: LocalDate? = null,
  val recallEmailReceivedDateTime: OffsetDateTime? = null,
  val localPoliceForce: String? = null,
  val contrabandDetail: String? = null,
  val vulnerabilityDiversityDetail: String? = null,
  val mappaLevel: MappaLevel? = null,
  // sentencing info
  val sentenceDate: LocalDate? = null,
  val licenceExpiryDate: LocalDate? = null,
  val sentenceExpiryDate: LocalDate? = null,
  val sentencingCourt: String? = null,
  val indexOffence: String? = null,
  val conditionalReleaseDate: LocalDate? = null,
  val sentenceLength: Api.SentenceLength? = null,
  //
  val bookingNumber: String? = null,
  val probationOfficerName: String? = null,
  val probationOfficerPhoneNumber: String? = null,
  val probationOfficerEmail: String? = null,
  val probationDivision: ProbationDivision? = null,
  val authorisingAssistantChiefOfficer: String? = null,
  val currentPrison: String? = null
)

enum class RecallLength {
  FOURTEEN_DAYS,
  TWENTY_EIGHT_DAYS
}

enum class MappaLevel {
  NA,
  LEVEL_1,
  LEVEL_2,
  LEVEL_3,
  NOT_KNOWN,
  CONFIRMATION_REQUIRED
}

enum class RecallType {
  FIXED
}

enum class ProbationDivision {
  LONDON,
  MIDLANDS,
  NORTH_EAST,
  NORTH_WEST,
  SOUTH_EAST,
  SOUTH_WEST,
  SOUTH_WEST_AND_SOUTH_CENTRAL,
  WALES
}
